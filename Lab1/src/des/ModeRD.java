package des;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ModeRD implements Mode {
    private Cryption algorithm;
    private byte[] initializationVec;
    private BigInteger delta;
    private BigInteger initial;

    public ModeRD(Cryption algo, byte[] init){
        this.algorithm = algo;
        this.initializationVec = init;
        this.initial = new BigInteger(init);
        this.delta = new BigInteger(init, init.length/2, init.length/2);
    }
    @Override
    public byte[] encrypt(byte[] buffer, int len) {
        int index = 0;
        long shift = 1<<8;
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> encryptedBlocksFutures = new LinkedList<>();
        encryptedBlocksFutures.add(service.submit(() -> algorithm.encrypt(ByteBuffer.allocate(8).put(initial.toByteArray()).array())));
        for (int i = 0; i < len; i += 8) {
            byte[] initArray = initial.toByteArray();
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.put(initArray);
            initArray = byteBuffer.array();
            for (int j = 0; j < 8; j++) {
                buffer[index++] ^= initArray[j];
            }
            initial = initial.add(delta).mod(BigInteger.valueOf(shift));
        }
        for (int i = 0; i < len; i += 8) {
            byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 8);
            encryptedBlocksFutures.add(service.submit(() -> algorithm.encrypt(newBuf)));
        }
        service.shutdown();

        return getBytes(encryptedBlocksFutures);
    }

    @Override
    public byte[] decrypt(byte[] buffer, int len) {
        int index = 0;
        long shift = 1<<8;
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> decryptedBlocksFutures = new LinkedList<>();
        for (int i = 8; i < len; i += 8) {
            byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 8);
            decryptedBlocksFutures.add(service.submit(() -> algorithm.decrypt(newBuf)));
        }
        service.shutdown();

        byte[] resBytes = getBytes(decryptedBlocksFutures);

        for (int i = 0; i < len; i += 8) {
            byte[] initArray = initial.toByteArray();
            ByteBuffer byteBuffer = ByteBuffer.allocate(8);
            byteBuffer.put(initArray);
            initArray = byteBuffer.array();
            for (int j = 0; j < 8; j++) {
                resBytes[index++] ^= initArray[j];
            }
            initial = initial.add(delta).mod(BigInteger.valueOf(shift));
        }

        return resBytes;
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
            e.printStackTrace();
        }
        return resBuf;
    }


    @Override
    public void reset() {
        this.initial = new BigInteger(this.initializationVec);
    }
}
