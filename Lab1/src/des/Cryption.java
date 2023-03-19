package des;

public interface Cryption {
    public byte[] encrypt(byte[] array);
    public byte[] decrypt(byte[] array);
    public void setRoundKeys(byte[] key);
}
