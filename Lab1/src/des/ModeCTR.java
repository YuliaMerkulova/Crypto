package des;

import java.awt.image.AreaAveragingScaleFilter;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

public class ModeCTR implements Mode {

    private final Cryption algorithm;
    private final byte[] initializationVec;
    private byte[] prevBlock;
    private int halfLen;
    private int len;
    private int mod;

    private int ctr;

    public ModeCTR(Cryption algo, byte[] init){
        this.algorithm = algo;
        this.initializationVec = init;
        len = init.length;
        halfLen = len / 2;
        mod = 1 << halfLen;
        prevBlock = Arrays.copyOfRange(init, 0, init.length);
        ctr = 0;
    }

    @Override
    public byte[] encrypt(byte[] buffer, int len) {
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> encryptedBlocksFutures = new LinkedList<>();
        for (int i = 0; i < len; i += 8) {
            byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 8);
            encryptedBlocksFutures.add(service.submit(() -> {
                int shift = ctr;
                byte[] encCtr = algorithm.encrypt(shiftHalf(shift));
                for (int i1 = 0; i1 < 8; i1++) {
                    newBuf[i1] ^= encCtr[i1];
                }
                return newBuf;
            }));
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
            e.printStackTrace();
        }
        return resBuf;
    }

    @Override
    public byte[] decrypt(byte[] buffer, int len) {
        return encrypt(buffer, len);
    }

    @Override
    public void reset() {
        ctr = 0;
    }

    private byte[] shiftHalf(int shift) {
        BigInteger half = new BigInteger(initializationVec, halfLen, halfLen);
        int intHalf = half.intValue();
        intHalf = (intHalf + shift) % mod;
        ByteBuffer buf = ByteBuffer.allocate(halfLen);
        buf.putInt(intHalf);
        int ind = halfLen;
        byte[] res = Arrays.copyOfRange(initializationVec, 0, len);
        for (var curByte : buf.array()) {
            res[ind++] = curByte;
        }
        return res;
    }
}
