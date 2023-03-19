package des;

public interface Encrypt {
    public byte[] encryptBlock(byte[] array, byte[] key);
}
