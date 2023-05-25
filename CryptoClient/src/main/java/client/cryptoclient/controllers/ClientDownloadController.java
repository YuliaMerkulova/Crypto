package client.cryptoclient.controllers;

import client.cryptoclient.algorithms.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.*;

import static client.cryptoclient.controllers.ClientController.intArrayToByte;

@Controller
@Slf4j
public class ClientDownloadController {

    @Autowired
    ObjectMapper objectMapper;
    private static final Random randomizer = new Random(LocalDateTime.now().getNano());

    private final ConcurrentMap<Integer, BenalohCipher> myCiphers = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, Crypto> clientsProgressBarDownload = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, ByteArrayResource> clientsDownloadFile = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, States> clientsStateDownload = new ConcurrentHashMap<>();


    @GetMapping("/downloading/{fileId}/{clientId}")
    public ResponseEntity<Object> gettingFileFromServer(@PathVariable("fileId") Integer fileId,
                                        @PathVariable("clientId") Integer clientId) throws JsonProcessingException {

        BenalohCipher benalohCipher = new BenalohCipher(128);

        myCiphers.put(clientId, benalohCipher);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        PublicKey publicKey = benalohCipher.getPublicKey();

        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("key", objectMapper.writeValueAsString(publicKey));
        body.add("id", fileId);
        body.add("clientId", clientId);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        //clientsStateDownload.put(clientId, States.WORKING);
        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/crypto/getFile", HttpMethod.POST, requestEntity, String.class);

        if (response.getStatusCode().is3xxRedirection()){
            log.error("FILE NOT FOUND!");
            clientsStateDownload.put(clientId, States.NOT_FOUND);
        }
        Map<String, String> responseMap = new HashMap<>();
        responseMap.put("status", "ok");
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @GetMapping("/download{id}") // ловим с html какой файлик мы хотим скачать и отправляем этот запрос на сервер
    public ResponseEntity<Object> wantDownloadFile(Model model, @PathVariable("id") Integer id) {
        HashMap<String, Integer> body = new HashMap<>();
        //model.addAttribute("fileId", id);
        //model.addAttribute("title", "Download: " + id);
        int myId;
        do {
            myId = randomizer.nextInt();
        } while(myCiphers.containsKey(myId));
        model.addAttribute("myId", myId);
        log.info("Начинаем работу по скачиванию!");
        clientsStateDownload.put(myId, States.WAITING);
        return "download";
    }


    @PostMapping("/downloadFile")
    public ResponseEntity<Object> downloadingFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("key") String key,
                                                  @RequestParam("clientId") Integer clientId,
                                                  @RequestParam("IV") String IV,
                                                  @RequestParam("mode") String mode) throws IOException {
        BigInteger[] encKey = objectMapper.readValue(key, BigInteger[].class);
        BigInteger[] encIV = objectMapper.readValue(IV, BigInteger[].class);
        int[] decryptedKey = new int[0];
        int[] decryptedIV = new int[0];
        clientsStateDownload.put(clientId, States.WORKING);

        if (myCiphers.containsKey(clientId)){
            BenalohCipher benalohCipher = myCiphers.get(clientId);
            decryptedKey = benalohCipher.decryptKey(encKey);
            decryptedIV = benalohCipher.decryptKey(encIV);
        }
        byte[] bytesFile;
        byte[] arrayIV = intArrayToByte(decryptedIV);
        SerpentCipher serpentCipher = new SerpentCipher(decryptedKey);
        Crypto cryptoProcess = new Crypto(Modes.valueOf(mode), serpentCipher, arrayIV);

        clientsProgressBarDownload.put(clientId, cryptoProcess);

        long sizeFile = file.getSize();

        log.info("Начинаем расшифровку!");
        bytesFile = cryptoProcess.decryptFile(file.getInputStream(), sizeFile);
        ByteArrayResource resource = new ByteArrayResource(bytesFile) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };

        clientsDownloadFile.put(clientId, resource);
        ExecutorService service = Executors.newSingleThreadExecutor();
        service.submit(() -> {
            try {
                Thread.sleep(180000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            clientsDownloadFile.remove(clientId, resource);
        });
        clientsStateDownload.put(clientId, States.FINISHED);

        return ResponseEntity.ok().build();
    }


    @GetMapping("/progress{id}")
    public ResponseEntity<Object> progress(@PathVariable("id") Integer id){
        Map<String, Object> body = new HashMap<>();
        //Float progress = 0F;
        log.info("client id = " + id);
        Crypto myCrypto = clientsProgressBarDownload.get(id);
        States state = clientsStateDownload.get(id);
        if (Optional.ofNullable(state).isEmpty()){
            body.put("progress", 0);
            body.put("state", States.NOT_FOUND.toString());
            //body.put("url", "/filenotfound");
        }
        if (clientsStateDownload.get(id) == States.NOT_FOUND) {
            body.put("state", States.NOT_FOUND.toString());
            body.put("url", "/filenotfound");
        }
        else if (clientsStateDownload.get(id) == States.FINISHED){
            body.put("progress", 100);
            body.put("state", States.FINISHED.toString());
            clientsStateDownload.remove(id);
            clientsProgressBarDownload.remove(id);
        } else if (clientsStateDownload.get(id) == States.WORKING || clientsStateDownload.get(id) == States.WAITING ) {
            if (Optional.ofNullable(myCrypto).isEmpty()) {
                body.put("progress", 0);
                body.put("state", States.WAITING.toString());
            }
            else {
                body.put("progress", myCrypto.getDecryptProgress());
                body.put("state", States.WORKING.toString());
            }
        }

        return new ResponseEntity<>(body, HttpStatusCode.valueOf(200));
    }


    @GetMapping("/downloadFileClient{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer id) throws IOException {

        Resource fileResource = clientsDownloadFile.remove(id);

        // Установка заголовков ответа
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(fileResource.getFilename(), StandardCharsets.UTF_8));

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(fileResource);
    }




}
