package des;

import java.util.Arrays;

public class ModeCFB implements Mode {
    private Cryption algorithm;
    private byte[] initializationVec;
    private byte[] prevBlock;
    public ModeCFB(Cryption algo, byte[] init){
        this.algorithm = algo;
        this.initializationVec = init;
        this.prevBlock = initializationVec;
    }
    public void reset() {
        prevBlock = initializationVec;
    }
    public byte[] encrypt(byte[] buffer, int len) {
        byte[] resBuf = new byte[80000];
        int index = 0;
        try {
            for (int i = 0; i < len; i += 8) {
                byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 8);
                prevBlock = algorithm.encrypt(prevBlock);

                for(int j = 0; j < 8; j++) {
                    prevBlock[j] = (byte) (newBuf[j] ^ prevBlock[j]);
                }
                for (int k = 0; k < 8; k++) {
                    resBuf[index++] = prevBlock[k];
                }
            }
        }
        catch (MyException e) {
            //asmfa;slmlm
        }
        return resBuf;
    }
    public byte[] decrypt(byte[] buffer, int len){
        byte[] resBuf = new byte[80000];
        int index = 0;
        try {
            for (int i = 0; i < len; i += 8) {
                byte[] encryptedPrev = algorithm.encrypt(prevBlock);
                prevBlock = Arrays.copyOfRange(buffer, i, i + 8);

                for(int j = 0; j < 8; j++) {
                    encryptedPrev[j] = (byte) (prevBlock[j] ^ encryptedPrev[j]);
                }
                for (int k = 0; k < 8; k++) {
                    resBuf[index++] = encryptedPrev[k];
                }
            }
        }
        catch (MyException e) {
            //asmfa;slmlm
        }
        return resBuf;
    }
}
