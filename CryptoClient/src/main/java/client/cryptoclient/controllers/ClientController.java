package client.cryptoclient.controllers;

import client.cryptoclient.algorithms.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.ibm.icu.text.Transliterator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import static client.cryptoclient.algorithms.BenalohCipher.encryptKey;
@Controller
@Slf4j
public class ClientController {
    @Autowired
    ObjectMapper objectMapper;
    private static final Random randomizer = new Random(LocalDateTime.now().getNano());
    private final ConcurrentMap<Integer, BenalohCipher> myCiphers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Crypto> clientsProgressBarDownload = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, Crypto> clientsProgressBarUpload = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, ByteArrayResource> clientsDownloadFile = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, States> clientsStateDownload = new ConcurrentHashMap<>();
    private final ConcurrentMap<Integer, States> clientsStateUpload = new ConcurrentHashMap<>();
    @AllArgsConstructor
    public static class RecordModel{
        public String fileName;
        public Integer id;

    }

    @GetMapping("/")
    public String getHome(Model model){
        model.addAttribute("home", "kokoko");
        RestTemplate restTemplate = new RestTemplate();
        List<RecordModel> files = restTemplate.getForObject("http://localhost:8080/crypto/getfiles", List.class);
        System.out.println(files.toString() + " " +files.size());
        model.addAttribute("files", files);

        return "MainPage";
    }
    @GetMapping("/filenotfound")
    public String fileNotFount(Model model){
        return "NotFound";
    }

    @GetMapping("/download{id}")
    public String wantDownloadFile(Model model, @PathVariable("id") Integer id, HttpServletResponse responseClient) throws JsonProcessingException {
        model.addAttribute("title", "Download: " + id);
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        BenalohCipher benalohCipher = new BenalohCipher(128);

        int myId;
        do {
            myId = randomizer.nextInt();
        } while(myCiphers.containsKey(myId));

        model.addAttribute("myId", myId);

        myCiphers.put(myId, benalohCipher);


        PublicKey publicKey = benalohCipher.getPublicKey();

        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        System.out.println("key = " + objectMapper.writeValueAsString(publicKey));
        System.out.println("id =  " + id);
        System.out.println("clientId = " + myId);
        body.add("key", objectMapper.writeValueAsString(publicKey));
        body.add("id", id);
        body.add("clientId", myId);
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        int finalMyId = myId;
        clientsStateDownload.put(finalMyId, States.WORKING);
        CompletableFuture.supplyAsync(() -> {
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/crypto/getFile", HttpMethod.POST, requestEntity, String.class);
            return response.getStatusCode();
        }).thenAccept(result -> {
            log.warn(":::" +  result.is3xxRedirection() + " " +  result.value());
            if (result.is3xxRedirection()){
                clientsStateDownload.put(finalMyId, States.NOT_FOUND);
            }

        });
        log.warn("HAHAHAHHHAHHAHAHAHAHAH");
        return "download";
    }

    @PostMapping("/downloadFile")
    public ResponseEntity<Object> downloadingFile(@RequestParam("file") MultipartFile file,
                                                  @RequestParam("key") String key,
                                                  @RequestParam("clientId") Integer clientId,
                                                  @RequestParam("IV") String IV,
                                                  @RequestParam("mode") String mode) throws IOException, ExecutionException, InterruptedException {
        BigInteger[] encKey = objectMapper.readValue(key, BigInteger[].class);
        BigInteger[] encIV = objectMapper.readValue(IV, BigInteger[].class);
        int[] decryptedKey = new int[0];
        int[] decryptedIV = new int[0];

        if (myCiphers.containsKey(clientId)){
            BenalohCipher benalohCipher = myCiphers.get(clientId);
            //myCiphers.remove(clientId);
            decryptedKey = benalohCipher.decryptKey(encKey);
            decryptedIV = benalohCipher.decryptKey(encIV);
        }
        byte[] bytesFile;
        byte[] arrayIV = intArrayToByte(decryptedIV);
        SerpentCipher serpentCipher = new SerpentCipher(decryptedKey);
        Crypto cryptoProcess = new Crypto(Modes.valueOf(mode), serpentCipher, arrayIV);

        clientsProgressBarDownload.put(clientId, cryptoProcess);
        long sizeFile = file.getSize();
        bytesFile = cryptoProcess.decryptFile(file.getInputStream(), sizeFile);
        ByteArrayResource resource = new ByteArrayResource(bytesFile) {
            @Override
            public String getFilename() {
                return file.getOriginalFilename();
            }
        };
        clientsDownloadFile.put(clientId, resource);
        clientsStateDownload.put(clientId, States.FINISHED);
        return ResponseEntity.ok().build();
    }



    @GetMapping("/progress{id}")
    public ResponseEntity<Object> progress(@PathVariable("id") Integer id){
        Map<String, Object> body = new HashMap<>();
        Float progress = 0F;
        log.info("client id = " + id);
        Crypto myCrypto = clientsProgressBarDownload.get(id);
        States state = clientsStateDownload.get(id);
        if (Optional.ofNullable(myCrypto).isEmpty() || Optional.ofNullable(state).isEmpty()){
            body.put("progress", 100);
            body.put("state", States.FINISHED.toString());
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
        } else if (clientsStateDownload.get(id) == States.WORKING) {
            if (Optional.ofNullable(myCrypto).isEmpty()) {
                body.put("progress", 0);
            }
            else {
                body.put("progress", myCrypto.getDecryptProgress());
            }
            body.put("state", States.WORKING.toString());
        }

        return new ResponseEntity<>(body, HttpStatusCode.valueOf(200));
    }

    @GetMapping("/upload")
    public String upload(Model model){
        model.addAttribute("title", "kokoko");
        return "UploadFile";
    }

    @PostMapping("/upload")
    public @ResponseBody RedirectView handleFileUpload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("mode") String mode) throws IOException {
        Integer id = null;
        if (!file.isEmpty()) {
            int[] key = generateKey(8);
            SerpentCipher serpentCipher = new SerpentCipher(key);
            URL url = new URL("http://localhost:8080/crypto/getKey");
            URLConnection connection = url.openConnection();
            HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
            httpURLConnection.setRequestMethod("GET");
            int responseCode = httpURLConnection.getResponseCode();
            PublicKey publicKey = new PublicKey();
            if (responseCode == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));

                String inputLine;
                StringBuffer getKey = new StringBuffer();
                while ((inputLine = in.readLine()) != null) {
                    getKey.append(inputLine);
                }
                in.close();
                JsonNode node = objectMapper.readTree(getKey.toString());
                id = node.get("id").asInt();
                publicKey = objectMapper.readValue(node.get("public").asText(), PublicKey.class);
                Crypto cryptoProcess = new Crypto(Modes.valueOf(mode), serpentCipher);
                clientsProgressBarUpload.put(id, cryptoProcess);
                clientsStateUpload.put(id, States.WORKING);
                log.info("Доьавили штуки");
                Integer finalId = id;
                PublicKey finalPublicKey = publicKey;
                log.info("Создаем поток");
                ExecutorService executorService = Executors.newSingleThreadExecutor();

                executorService.submit(() -> {
                    try {
                        log.info("Начинается поток");
                        long sizeFile = file.getSize();
                        log.error("SIZE FILE IS  " + file.getSize());
                        log.info("Зашифровываем файл");
                        byte[] bytes = new byte[0];

                        try{
                            bytes = cryptoProcess.encryptFile(file.getInputStream(), sizeFile);
                        } catch (Exception e)
                        {
                            log.error("WE HAVE GOT AN ERROR");
                            e.printStackTrace();
                        }
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
                        body.add("id", finalId);
                        body.add("mode", mode);
                        body.add("IV", objectMapper.writeValueAsString(encryptKey(cryptoProcess.getIV(), finalPublicKey)));
                        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
                        log.info("Отправляем на сервер");
                        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/crypto/upload",
                                HttpMethod.POST, requestEntity, String.class);
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            clientsStateUpload.put(finalId, States.NOT_FOUND);
                        }
                        clientsStateUpload.put(finalId, States.FINISHED);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } catch (RestClientException e) {
                        throw new RuntimeException(e);
                    }

                });
                executorService.shutdown();
            }

        }
        else{
            RedirectView redirectView = new RedirectView("/filenotfound");
            return redirectView;
        }
        System.out.println("myID = " + id);
        RedirectView redirectView = new RedirectView("/uploadingFile" + id);
        return redirectView;
    }

    @GetMapping("/uploadingFile{id}")
    public String uplooading(Model model, @PathVariable Integer id){
        System.out.println("myId = " + id);
        model.addAttribute("myId", id);
        return "uploading";
    }


    @GetMapping("/progressUpload{id}")
    @ResponseBody
    public ResponseEntity getUploadProgress(@PathVariable("id") Integer id) {
        // Получите данные прогресса, например, из сервиса или другого источника
        Map<String, Object> body = new HashMap<>();
        Float progress = 0F;
        log.info("client id = " + id);
        Crypto myCrypto = clientsProgressBarUpload.get(id);
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

    @GetMapping("/downloadFileClient{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Integer id) throws IOException {

        // Создание объекта Resource для представления файла
        Resource fileResource = clientsDownloadFile.remove(id);

        // Установка заголовков ответа
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLEncoder.encode(fileResource.getFilename(), StandardCharsets.UTF_8));

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(fileResource);
    }

    public static int[] generateKey(int len) {  // len = 4/6/8
        int[] key = new int[len];
        for (int i = 0; i < len; i++) {
            key[i] = randomizer.nextInt();
        }
        return key;
    }

    public static byte[] intArrayToByte(int[] array){
        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
        for (int i = 0; i < array.length; i++){
            buffer.putInt(array[i]);
        }
        return buffer.array();
    }

}
