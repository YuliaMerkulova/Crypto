package client.cryptoclient.algorithms;

public class ModeCBC implements CypherMode {
    private byte[] IV;
    private SerpentCipher serpentCipher;
    public ModeCBC(SerpentCipher serpentCipher, byte[] IV) {
        this.serpentCipher = serpentCipher;
        this.IV = IV;
    }
    @Override
    public byte[] encrypt(byte[] data, int len) {
        System.out.println("Стар шифроания");
        byte[] prevBlock = IV;
        for (int i = 0; i < data.length; i+=16) {
            System.out.println("В цикле " + i);
            byte[] block = xor(data, prevBlock, i);
            prevBlock = serpentCipher.encrypt(block);
            System.arraycopy(prevBlock, 0, data, i, 16);
        }
        System.out.println("После шифрования");
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
        for (int i = 0; i < data.length; i += 16) {
            byte[] block = new byte[16];
            System.arraycopy(data, i, block, 0, 16);
            byte[] tmp = block;
            block = serpentCipher.decrypt(block);
            block = xor(block, prevBlock, 0);
            prevBlock = tmp;
            System.arraycopy(block, 0, data, i, 16);
        }
        return data;
    }
    @Override
    public void reset() {}
}
