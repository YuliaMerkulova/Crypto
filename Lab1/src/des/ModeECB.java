package des;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ModeECB implements Mode{
    private Cryption algorithm;
    public ModeECB(Cryption algo){
        this.algorithm = algo;
    }
    public void reset(){}
    public byte[] encrypt(byte[] buffer, int len){
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> encryptedBlocksFutures = new LinkedList<>();
        for (int i = 0; i < len; i += 8){
            byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 8);
            encryptedBlocksFutures.add(service.submit(() -> algorithm.encrypt(newBuf)));
        }
        service.shutdown();

        return getBytes(encryptedBlocksFutures);
    }

    public byte[] decrypt(byte[] buffer, int len){
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> encryptedBlocksFutures = new LinkedList<>();
        for (int i = 0; i < len; i += 8){
            byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 8);
            encryptedBlocksFutures.add(service.submit(() -> algorithm.decrypt(newBuf)));
        }
        service.shutdown();
        return getBytes(encryptedBlocksFutures);
    }

    private byte[] getBytes(List<Future<byte[]>> encryptedBlocksFutures) {
        byte[] resBuf = new byte[80000];
        int index = 0;
        try {
            for (var futureBufToWrite : encryptedBlocksFutures) {
                byte[] encryptedBuf = futureBufToWrite.get();
                for (int i = 0; i < 8; i++) {
                    resBuf[index++] = encryptedBuf[i];
                }
            }
        }
        catch (ExecutionException | InterruptedException e) {
            //dkdslkndsklnds
        }
        return resBuf;
    }
}
