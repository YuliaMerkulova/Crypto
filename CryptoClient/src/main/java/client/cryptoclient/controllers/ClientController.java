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
    //private final ConcurrentMap<Integer, Crypto> clientsProgressBarUpload = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, ByteArrayResource> clientsDownloadFile = new ConcurrentHashMap<>();

    private final ConcurrentMap<Integer, States> clientsStateDownload = new ConcurrentHashMap<>();
    //private final ConcurrentMap<Integer, States> clientsStateUpload = new ConcurrentHashMap<>();
    @AllArgsConstructor
    public static class RecordModel{
        public String fileName;
        public String Mode;
        public Float size;
        public Date dateUploding;
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
