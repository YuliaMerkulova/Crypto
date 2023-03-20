package des;

public interface Cryption {
    public byte[] encrypt(byte[] array);
    public byte[] decrypt(byte[] array);
    public byte[][] setRoundKeys(byte[] key);
}
