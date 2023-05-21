package client.cryptoclient.algorithms;

public interface CypherMode {
    public byte[] encrypt(byte[] data, int len);
    public byte[] decrypt(byte[] data, int len);
    public void reset();
}
