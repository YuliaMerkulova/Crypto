package client.cryptoclient.algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ModeECB implements CypherMode {
    private SerpentCipher serpentCipher;
    public ModeECB(SerpentCipher serpentCipher) {
        this.serpentCipher = serpentCipher;
    }
    @Override
    public byte[] encrypt(byte[] data, int len) {
        List<Future<byte[]>> encryptTasks = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i < len; i += 16) {
            byte[] block = Arrays.copyOfRange(data, i, i + 16);
            encryptTasks.add(executorService.submit(() -> serpentCipher.encrypt(block)));
        }
        executorService.shutdown();
        for (int i = 0; i < len / 16; i += 1) {
            try {
                byte[] block = encryptTasks.get(i).get();
                System.arraycopy(block, 0, data, i * 16, 16);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        return data;
    }

    @Override
    public byte[] decrypt(byte[] data, int len){
        List<Future<byte[]>> decryptTasks = new ArrayList<>();

        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i < len; i += 16) {
            byte[] block = Arrays.copyOfRange(data, i, i + 16);
            decryptTasks.add(executorService.submit(() -> serpentCipher.decrypt(block)));
        }
        try{
            for (int i = 0; i < len / 16; i++) {
                byte[] block = decryptTasks.get(i).get();
                System.arraycopy(block, 0, data, i * 16, 16);
            }
        } catch (ExecutionException | InterruptedException e){
            e.printStackTrace();
        }
        return data;
    }
    @Override
    public void reset() {}
}
