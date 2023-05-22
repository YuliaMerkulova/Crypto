package server.cryptoserver.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import server.cryptoserver.algorithms.BenalohCipher;
import server.cryptoserver.algorithms.PublicKey;
import server.cryptoserver.models.MyRecord;
import server.cryptoserver.repository.RecordRepository;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.StreamSupport;

@Slf4j
@Controller // эта штука будет ловить запросы
@RequestMapping(path = "/crypto") //путь по которому стучимся
public class CryptoController {
    private ConcurrentHashMap<Integer, BenalohCipher> clients = new ConcurrentHashMap<>();
    ObjectMapper objectMapper = new ObjectMapper();
    private Random randomizer = new Random(LocalDateTime.now().getNano());
    public static class RecordModel{
        public String fileName;
        public int id;

        public RecordModel(MyRecord record){
            fileName = record.getFileName();
            id = record.getId();
        }
    }

    @GetMapping("/getKey")
    public ResponseEntity<Object> getPublicKey(){
        //сгенерировать public private Benaloh у юзера есть айдишник
        // нужно запомнить какой ключ кому
        BenalohCipher benalohCipher = new BenalohCipher(128);
        Integer id;
        do {
            id = randomizer.nextInt();
        } while(clients.containsKey(id));
        clients.put(id, benalohCipher);
        Map<String, Object> mapResponse = new HashMap<>();
        mapResponse.put("id", id);
        try {
            mapResponse.put("public", objectMapper.writeValueAsString(benalohCipher.getPublicKey()));
        } catch (JsonProcessingException e){
            log.error("CANT");
        }
        log.info("HURRAY");
        return new ResponseEntity<>(mapResponse, HttpStatusCode.valueOf(200));
    }

    @GetMapping("/getfiles")
    public ResponseEntity<Object> getFiles(){
        var files = StreamSupport.stream(recordRepository.findAll().spliterator(), false).map(RecordModel::new).toArray();
        return new ResponseEntity<>(files, HttpStatusCode.valueOf(200));
    }

    @PostMapping("/getFile")
    public ResponseEntity<Object> getFileFrom(@RequestParam("key") String key, @RequestParam("id") Integer id, @RequestParam("clientId") Integer clientId) throws JsonProcessingException {
        //нам приходит public ключ Benaloh мы им зашифровывем ключ из БД
        // отправляем файл + заш(ключ) клиентику
        log.info("Кто-то  хочет скачать файл");
        PublicKey publicKey = objectMapper.readValue(key, PublicKey.class);
        MyRecord record = recordRepository.findById(id).orElse(null);
        log.info("Ищем файлик");
        String responses = "lalala";
        if (Optional.ofNullable(record).isEmpty()){
            log.error("FILE NOT FOUND");
            return ResponseEntity.status(300).build();
        }

        byte[] serpentKey = record.getKey_().getBytes(StandardCharsets.ISO_8859_1);
        byte[] IV = record.getIV().getBytes(StandardCharsets.ISO_8859_1);
        log.info("Делаем буфер");
//        ByteBuffer buf = ByteBuffer.wrap(serpentKey);
//        int[] keySerpent = new int[serpentKey.length / 4];
//        for (int i = 0; i < keySerpent.length; i++){
//            keySerpent[i] = buf.getInt();
//        }
        log.info("Зашифровываем ключ");
        BigInteger[] encryptedKey = BenalohCipher.encryptKey(serpentKey, publicKey);
        BigInteger[] encryptedIV = BenalohCipher.encryptKey(IV, publicKey);
        String jsonKey = objectMapper.writeValueAsString(encryptedKey);
        String jsonIV = objectMapper.writeValueAsString(encryptedIV);
        log.info("Создаем заголовочки");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        ByteArrayResource resource = new ByteArrayResource(record.getFile()){
            @Override
            public String getFilename(){return record.getFileName();}
        };

        RestTemplate restTemplate = new RestTemplate();
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        log.info("Пишем тельце");
        body.add("key", jsonKey);
        body.add("file", resource);
        body.add("clientId", clientId);
        body.add("IV", jsonIV);
        body.add("mode", record.getMode());
        log.info("Собираемся отправить");
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        var response = restTemplate.postForEntity("http://localhost:8081/downloadFile", requestEntity, Object.class);

        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    @PostMapping("/upload")
    public ResponseEntity<Object> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam("id") Integer id,
                                             @RequestParam("key") String key,
                                             @RequestParam("mode") String mode,
                                             @RequestParam("IV") String IV) throws IOException {
        //System.out.println(key);
        BigInteger[] encKey = objectMapper.readValue(key, BigInteger[].class);
        BigInteger[] encIV = objectMapper.readValue(IV, BigInteger[].class);
        //System.out.println(Arrays.toString(encKey));
        int[] decryptedKey = new int[0];
        int[] decryptedIV = new int[0];
        if (clients.containsKey(id)){
            BenalohCipher benalohCipher = clients.get(id);
            clients.remove(id);
            decryptedKey = benalohCipher.decryptKey(encKey);
            decryptedIV = benalohCipher.decryptKey(encIV);
            System.out.println(Arrays.toString(decryptedKey));
        }

        String keyString = new String(intArrayToByte(decryptedKey), StandardCharsets.ISO_8859_1);
        String IVString = new String(intArrayToByte(decryptedIV), StandardCharsets.ISO_8859_1);
        byte[] fileRec = file.getBytes();
        MyRecord record = new MyRecord(fileRec, keyString, file.getOriginalFilename(), IVString, mode);
        recordRepository.save(record);
        log.info("save file");
        return new ResponseEntity<>(HttpStatusCode.valueOf(200));
    }

    public static byte[] intArrayToByte(int[] array){
        ByteBuffer buffer = ByteBuffer.allocate(array.length * 4);
        for (int i = 0; i < array.length; i++){
            buffer.putInt(array[i]);
        }
        return buffer.array();
    }


    private RecordRepository recordRepository;

    public CryptoController(@Autowired RecordRepository recordRepository){
        this.recordRepository = recordRepository;
    }
}
