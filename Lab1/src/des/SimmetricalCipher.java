package des;

public abstract class SimmetricalCipher {

    private byte[] key;
    ModesCipher mode;
    protected SimmetricalCipher(byte[] key, ModesCipher mode, int vector){
        this.key = key;
        this.mode = mode;
    }

    public abstract void encryptData(byte[] array, byte[] resArray);
    public abstract void decryptData(byte[] array, byte[] resArray);
}
