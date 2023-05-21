package client.cryptoclient.algorithms;

import java.util.Arrays;

public class ModeCFB implements CypherMode {
    private byte[] IV;
    private SerpentCipher serpentCipher;
    public ModeCFB(SerpentCipher serpentCipher, byte[] IV) {
        this.serpentCipher = serpentCipher;
        this.IV = IV;
    }
    @Override
    public byte[] encrypt(byte[] data, int len) {
        byte[] prevBlock = IV;
        for (int i = 0; i < data.length; i+=16) {
            prevBlock = xor(data, serpentCipher.encrypt(prevBlock), i);
            System.arraycopy(prevBlock, 0, data, i, 16);
        }
        return data;
    }

    private byte[] xor(byte[] a, byte[] b, int pos) {
        byte[] result = new byte[b.length];
        for (int i = 0; i < b.length; i++) {
            result[i] = (byte) (a[pos + i] ^ b[i]);
        }
        return result;
    }

    @Override
    public byte[] decrypt(byte[] data, int len) {
        byte[] prevBlock = IV;
        for (int i = 0; i < data.length; i+=16) {
            byte[] block = Arrays.copyOfRange(data, i, i + 16);
            prevBlock = xor(block, serpentCipher.encrypt(prevBlock), 0);
            System.arraycopy(prevBlock, 0, data, i, 16);
            prevBlock = block;
        }
        return data;
    }
    @Override
    public void reset() {}
}
