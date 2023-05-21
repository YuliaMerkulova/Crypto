package client.cryptoclient.controllers;

import client.cryptoclient.algorithms.*;
import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static client.cryptoclient.algorithms.BenalohCipher.encryptKey;

@Controller
@Slf4j
public class ClientController {
    @Autowired
    ObjectMapper objectMapper;
    private static final Random randomizer = new Random(LocalDateTime.now().getNano());
    private final Map<Integer, BenalohCipher> myCiphers = new HashMap<Integer, BenalohCipher>();
    private final Map<Integer, Crypto> clientsProgressBar = new HashMap<Integer, Crypto>();
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

    @GetMapping("/download{id}")
    public String wantDownloadFile(Model model, @PathVariable("id") Integer id) throws JsonProcessingException {
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

        ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/crypto/getFile", HttpMethod.POST, requestEntity, String.class);
        if (response.getStatusCode() == HttpStatus.OK){
            log.info("Your request approved");
        }
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

        clientsProgressBar.put(clientId, cryptoProcess);

        bytesFile = cryptoProcess.decryptFile(file.getInputStream());
        //bytesFile = serpentCipher.decryptData(bytesFile);
        try{
            File myFile = new File("D:/" + file.getOriginalFilename());
            FileOutputStream fos = new FileOutputStream(myFile);
            fos.write(bytesFile);
            fos.close();
            log.info("File successfully saved!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return ResponseEntity.ok().build();
    }



    @GetMapping("/progress{id}")
    public ResponseEntity<Object> progress(@PathVariable("id") Integer id){
        Map<String, Object> body = new HashMap<>();
        Float progress = 0F;
        body.put("progress", progress);
        body.put("success", false);

        return new ResponseEntity<>(body, HttpStatusCode.valueOf(200));
    }

    @PostMapping("/upload")
    public @ResponseBody String handleFileUpload(@RequestParam("file") MultipartFile file,
                                                 @RequestParam("mode") String mode){
        if (!file.isEmpty()) {
            try {
                int[] key = generateKey(8);

                //System.out.println("Key generated");
                SerpentCipher serpentCipher = new SerpentCipher(key);
                file.getSize();
                //System.out.println(Arrays.toString(key));
                //bytes = serpentCipher.encryptFile(bytes);
                Crypto cryptoProcess = new Crypto(Modes.valueOf(mode), serpentCipher);
                //System.out.println("file encrypted");
                byte[] bytes = cryptoProcess.encryptFile(file.getInputStream());
                URL url = new URL("http://localhost:8080/crypto/getKey");
                URLConnection connection = url.openConnection();
                HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
                httpURLConnection.setRequestMethod("GET");
                int responseCode = httpURLConnection.getResponseCode();
                //System.out.println("response "+  responseCode);
                if (responseCode == 200){
                    BufferedReader in = new BufferedReader(new InputStreamReader(httpURLConnection.getInputStream()));
                    String inputLine;
                    StringBuffer getKey = new StringBuffer();
                    while((inputLine = in.readLine()) != null){
                        getKey.append(inputLine);
                    }
                    in.close();
                    JsonNode node = objectMapper.readTree(getKey.toString());
                    Integer id = node.get("id").asInt();
                    PublicKey publicKey = objectMapper.readValue(node.get("public").asText(), PublicKey.class);

                    ByteArrayResource resource = new ByteArrayResource(bytes){
                        @Override
                        public String getFilename(){
                            return file.getOriginalFilename();
                        }
                    };

                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

                    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
                    body.add("file", resource);
                    System.out.println(publicKey.n);
                    BigInteger[] encKey = encryptKey(intArrayToByte(key), publicKey);
                    String jsonKey = objectMapper.writeValueAsString(encKey);
                    //System.out.println(jsonKey);
                    body.add("key", jsonKey);
                    body.add("id", id);
                    body.add("mode", mode);
                    body.add("IV", objectMapper.writeValueAsString(encryptKey(cryptoProcess.getIV(), publicKey)));
                    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

                    ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/crypto/upload",
                            HttpMethod.POST, requestEntity, String.class);

                    System.out.println("sended " + response.getStatusCode());
                }
                //шифруем файл, потом отправляем на сервер
                return "Вы удачно загрузили ";
            } catch (Exception e) {
                return "Вам не удалось загрузить " + " => " + e.getMessage();
            }
        } else {
            return "Вам не удалось загрузить " + " потому что файл пустой.";
        }
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
