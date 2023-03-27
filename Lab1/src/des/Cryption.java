package des;

public interface Cryption {
    public byte[] encrypt(byte[] array) throws MyException;
    public byte[] decrypt(byte[] array) throws MyException;
    public void setRoundKeys(byte[] key);
}
