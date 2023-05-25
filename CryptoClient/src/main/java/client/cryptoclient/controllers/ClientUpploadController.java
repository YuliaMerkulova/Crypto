package client.cryptoclient.controllers;

import client.cryptoclient.algorithms.Crypto;
import client.cryptoclient.algorithms.Modes;
import client.cryptoclient.algorithms.PublicKey;
import client.cryptoclient.algorithms.SerpentCipher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.view.RedirectView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static client.cryptoclient.algorithms.BenalohCipher.encryptKey;
import static client.cryptoclient.controllers.ClientController.generateKey;
import static client.cryptoclient.controllers.ClientController.intArrayToByte;

@Controller
@Slf4j
public class ClientUpploadController {
    @Autowired
    ObjectMapper objectMapper;
    private static final Random randomizer = new Random(LocalDateTime.now().getNano());
    private final ConcurrentMap<Integer, Crypto> clientsProgressBarUpload = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, States> clientsStateUpload = new ConcurrentHashMap<>();


    @GetMapping("/upload") //загружает страницу с формой обработка кнопки на главной странице
    public String upload(Model model){
        model.addAttribute("title", "kokoko");
        return "UploadFile";
    }

    @GetMapping("/newUploadUser")
    public ResponseEntity<Object> generateUserId(){
        Map<String, Integer> id = new HashMap<>();
        Integer myId;
        do{
           myId = randomizer.nextInt();
        } while(clientsProgressBarUpload.containsKey(myId) || clientsStateUpload.containsKey(myId));
        id.put("clientId", myId);
        return new ResponseEntity<>(id, HttpStatusCode.valueOf(200));
    }



    @PostMapping("/upload") // ловим с html какой файл с каким режимом мы хотим отправить на сервер
    public ResponseEntity<Object> handleFileUpload(@RequestParam("file") MultipartFile file,
                                                   @RequestParam("mode") String mode,
                                                   @RequestParam("clientId") Integer clientID) throws IOException {
        clientsStateUpload.put(clientID, States.WORKING);
        Integer id = null;
        Map<String, String> responseBody = new HashMap<>();
        if (!file.isEmpty()) {
            int[] key = generateKey(8);
            SerpentCipher serpentCipher = new SerpentCipher(key);
            URL url = new URL("http://localhost:8080/crypto/getKey");
            URLConnection connection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            httpURLConnection.setRequestMethod("GET");
            int responseCode = httpURLConnection.getResponseCode();

            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                String inputLine;
                StringBuffer getKey = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    getKey.append(inputLine);
                }
                in.close();
                PublicKey publicKey = new PublicKey();
                JsonNode node = objectMapper.readTree(getKey.toString());
                id = node.get("id").asInt();
                publicKey = objectMapper.readValue(node.get("public").asText(), PublicKey.class);
                Crypto cryptoProcess = new Crypto(Modes.valueOf(mode), serpentCipher);
                clientsProgressBarUpload.put(clientID, cryptoProcess);
                log.info("Добавили штуки");
                PublicKey finalPublicKey = publicKey;
                try {
                    long sizeFile = file.getSize();
                    log.info("SIZE FILE IS  " + file.getSize());
                    log.info("Зашифровываем файл");
                    byte[] bytes = new byte[0];
                    bytes = cryptoProcess.encryptFile(file.getInputStream(), sizeFile);
                    log.info("Закончили зашифровывать");
                    ByteArrayResource resource = new ByteArrayResource(bytes) {
                        @Override
                        public String getFilename() {
                            return file.getOriginalFilename();
                        }
                    };

                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("file", resource);

                    BigInteger[] encKey = encryptKey(intArrayToByte(key), finalPublicKey);
                    String jsonKey = objectMapper.writeValueAsString(encKey);

                    body.add("key", jsonKey);
                    body.add("id", id);
                    body.add("mode", mode);
                    body.add("IV", objectMapper.writeValueAsString(encryptKey(cryptoProcess.getIV(), finalPublicKey)));
                    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                    log.info("Отправляем на сервер");
                    ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/crypto/upload",
                            HttpMethod.POST, requestEntity, String.class);
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        clientsStateUpload.put(clientID, States.NOT_FOUND);
                    }
                    clientsStateUpload.put(clientID, States.FINISHED);
                } catch (IllegalArgumentException | IOException | RestClientException e) {
                    clientsStateUpload.put(clientID, States.NOT_FOUND);
                    throw new RuntimeException(e);
                }
            }
        }
        else{
            clientsStateUpload.put(clientID, States.NOT_FOUND);
            responseBody.put("status", "fileIsEmpty");
            return new ResponseEntity<>(responseBody, HttpStatusCode.valueOf(400));
        }
        responseBody.put("status", "ok");
        return new ResponseEntity<>(responseBody, HttpStatusCode.valueOf(200));
    }


    @GetMapping("/progressUpload{id}")
    public ResponseEntity getUploadProgress(@PathVariable("id") Integer id) {
        // Получите данные прогресса, например, из сервиса или другого источника
        Map<String, Object> body = new HashMap<>();
        Float progress = 0F;
        log.info("client id = " + id);
        Crypto myCrypto = clientsProgressBarUpload.get(id);
        States state = clientsStateUpload.get(id);
        if(myCrypto == null || state == null){
            body.put("state", States.WAITING.toString());
            body.put("url", "/filenotfound");
            body.put("progress", 0);
        }
        if (clientsStateUpload.get(id) == States.NOT_FOUND) {
            log.error("NOT FOUND");
            body.put("state", States.NOT_FOUND.toString());
            body.put("url", "/filenotfound");
        }
        else if (clientsStateUpload.get(id) == States.FINISHED){
            log.error("FINISHED");
            body.put("progress", 100);
            body.put("state", States.FINISHED.toString());
            clientsStateUpload.remove(id);
            clientsProgressBarUpload.remove(id);
        } else if (clientsStateUpload.get(id) == States.WORKING) {
            log.error("WORKING");

            if (Optional.ofNullable(myCrypto).isEmpty()) {
                log.error("EMPTY");
                body.put("progress", 0);
            }
            else {
                log.error("PUT PROGRESS = " + myCrypto.getEncryptProgress());
                body.put("progress", myCrypto.getEncryptProgress());
            }
            body.put("state", States.WORKING.toString());
        }

        return new ResponseEntity<>(body, HttpStatusCode.valueOf(200));
    }

}
