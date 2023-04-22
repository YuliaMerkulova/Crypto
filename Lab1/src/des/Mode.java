package des;

public interface Mode {
    public byte[] encrypt(byte[] buffer, int len);
    public byte[] decrypt(byte[] buffer, int len);
    public void reset();
}
