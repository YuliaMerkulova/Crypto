package des;

import java.util.Arrays;

public class ModeOFB implements Mode{
    private Cryption algorithm;
    private byte[] initializationVec;
    private byte[] prevBlock;
    public ModeOFB(Cryption algo, byte[] init){
        this.algorithm = algo;
        this.initializationVec = init;
        this.prevBlock = initializationVec;
    }
    @Override
    public byte[] encrypt(byte[] buffer, int len) {
        byte[] resBuf = new byte[80000];
        int index = 0;
        try {
            for (int i = 0; i < len; i += 8) {
                byte[] newBuf = Arrays.copyOfRange(buffer, i, i + 8);
                prevBlock = algorithm.encrypt(prevBlock);

                for(int j = 0; j < 8; j++) {
                    newBuf[j] = (byte) (newBuf[j] ^ prevBlock[j]);
                }
                for (int k = 0; k < 8; k++) {
                    resBuf[index++] = newBuf[k];
                }
            }
        }
        catch (MyException e) {
            //asmfa;slmlm
        }
        return resBuf;
    }

    @Override
    public byte[] decrypt(byte[] buffer, int len) {
        byte[] resBuf = new byte[80000];
        int index = 0;
        try {
            for (int i = 0; i < len; i += 8) {
                prevBlock = algorithm.encrypt(prevBlock);
                byte[] encryptedPrev = Arrays.copyOfRange(buffer, i, i + 8);

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

    @Override
    public void reset() {
        prevBlock = initializationVec;
    }
}
