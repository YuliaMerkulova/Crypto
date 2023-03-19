package des;

public abstract class SimmetricalCipher {
    private byte[] key;
    ModesCipher mode;
    public SimmetricalCipher(byte[] key, ModesCipher mode, int vector){
        this.key = key;
        this.mode = mode;
    };
    public abstract void encryptData();
    public abstract void decryptData();
}
