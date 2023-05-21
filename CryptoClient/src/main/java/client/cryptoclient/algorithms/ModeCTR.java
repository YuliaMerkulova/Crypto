package client.cryptoclient.algorithms;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ModeCTR implements CypherMode {
    private int len;
    private byte[] IV;
    private SerpentCipher serpentCipher;
    private int halfLen;
    private long mod;

    private int ctr;
    public ModeCTR(SerpentCipher serpentCipher, byte[] IV) {
        this.serpentCipher = serpentCipher;
        this.IV = IV;
        len = IV.length;
        halfLen = len / 2;
        mod = 1L << (halfLen * 8);
        ctr = 0;
    }
    @Override
    public byte[] encrypt(byte[] buffer, int len) {
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> encryptedBlocksFutures = new LinkedList<>();
            for (int i = 0; i < len; i += 16) {
            byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 16);
            encryptedBlocksFutures.add(service.submit(() -> {
                int shift = ctr++;
                byte[] encCtr = serpentCipher.encrypt(shiftHalf(shift));
                xor(newBuf, encCtr, 0);
                return newBuf;
            }));
        }
        service.shutdown();
        return getBytes(encryptedBlocksFutures, buffer);
    }

    private void xor(byte[] a, byte[] b, int pos) {
        for (int i = 0; i < 16; i++) {
            a[i] = (byte) (a[pos + i] ^ b[i]);
        }
    }

    private byte[] getBytes(List<Future<byte[]>> encryptedBlocksFutures, byte[] resBuf) {
        int index = 0;
        try {
            for (var futureBufToWrite : encryptedBlocksFutures) {
                byte[] encryptedBuf = futureBufToWrite.get();
                for (int i = 0; i < 16; i++) {
                    resBuf[index++] = encryptedBuf[i];
                }
            }
        }
        catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
        return resBuf;
    }

    private byte[] shiftHalf(int shift) {
        BigInteger half = new BigInteger(IV, halfLen, halfLen); // берём правую половину
        long longHalf = half.longValue(); // преобразуем в long
        longHalf = (longHalf + shift) % mod; // сдвигаем
        ByteBuffer buf = ByteBuffer.allocate(halfLen);
        buf.putLong(longHalf);
        byte[] res = Arrays.copyOfRange(IV, 0, len);  // берём IV, от него нам нужна левая половина
        System.arraycopy(buf.array(), 0, res, halfLen, halfLen); // дописываем в результат правую половину
        return res;
    }

    @Override
    public byte[] decrypt(byte[] buffer, int len) {
        return encrypt(buffer, len);
    }
    @Override
    public void reset() {
        ctr = 0;
    }
}
