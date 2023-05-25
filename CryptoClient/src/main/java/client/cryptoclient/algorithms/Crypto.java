package client.cryptoclient.algorithms;

import lombok.Getter;

import java.io.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Crypto {
    private CypherMode cypherMode;
    private Random randomizer = new Random(LocalDateTime.now().getNano());
    @Getter
    Float decryptProgress;
    @Getter
    double encryptProgress;

    //boolean isProgressBarEnd = false;
    @Getter
    private byte[] IV;
    private Modes mode;
    private SerpentCipher serpentCipher;

    public Crypto(Modes mode, SerpentCipher serpentCipher, byte[] IV) {
        this.mode = mode;
        this.IV = IV;
        this.serpentCipher = serpentCipher;
        setMode();
    }
    public Crypto(Modes mode, SerpentCipher serpentCipher) {
        this.mode = mode;
        IV = new byte[16];
        randomizer.nextBytes(IV);
        this.serpentCipher = serpentCipher;
        setMode();
    }
    private void setMode() {
        switch (mode) {
            case ECB -> cypherMode = new ModeECB(serpentCipher);
            case CBC -> cypherMode = new ModeCBC(serpentCipher, IV);
            case CTR -> cypherMode = new ModeCTR(serpentCipher, IV);
            case CFB -> cypherMode = new ModeCFB(serpentCipher, IV);
            case RD -> cypherMode = new ModeRD(serpentCipher, IV);
            case RDH -> cypherMode = new ModeRDH(serpentCipher, IV);
            case OFB -> cypherMode = new ModeOFB(serpentCipher, IV);
        }
    }
    public byte[] encryptFile(InputStream inputStream, long sizeFile) {

        System.out.println("Зашли в функцию");
        byte[] data = new byte[0];
        byte[] buffer = new byte[1048576];
        int len;
        encryptProgress = 0F;
        //System.out.println("SSIZE FILE " + sizeFile);
        double maxProgress = Math.ceil((double)sizeFile / (1048576)); // mb in file
        //System.out.println("MAX PROGRESS" + maxProgress);
        try {
            int maxLen = (this.mode == Modes.RD) ? 1048544 : (this.mode == Modes.RDH) ? 1048528 : 1048560;
            while ((len = inputStream.read(buffer, 0, maxLen)) > 0) {
                cypherMode.reset();
                int last = len % 16;
                Arrays.fill(buffer, len, len + 16 - last, (byte) (16 - last));
                //System.out.println("in encrypt padded = " + (buffer[len + 1] & 0xff));
                byte[] encryptedBlock = cypherMode.encrypt(buffer, len + 16 - last);
                byte[] tmp = new byte[data.length + len + 16 - last + ((this.mode == Modes.RD) ? 16 : (this.mode == Modes.RDH) ? 32 : 0)];
                System.arraycopy(data, 0, tmp, 0, data.length);
                System.arraycopy(encryptedBlock, 0, tmp, data.length, len + 16 - last + ((this.mode == Modes.RD) ? 16 : (this.mode == Modes.RDH) ? 32 : 0));
                data = tmp;
                //System.out.println("encryptProgress" + encryptProgress);
                encryptProgress += (1f/maxProgress) * 100;
                //System.out.println("encryptProgress" + encryptProgress);
            }
            //isProgressBarEnd = true;

        }
        catch  (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public byte[] decryptFile(InputStream inputStream, long sizeFile) {
        byte[] data = new byte[0];
        byte[] buffer = new byte[1048576];
        int len;
        decryptProgress = 0F;
        //isProgressBarEnd = false;
        Integer maxProgress = (int)Math.ceil(sizeFile / (1048576)); // mb in file

        try {
            while ((len = inputStream.read(buffer, 0, 1048576)) > 0) {
                cypherMode.reset();
                //System.out.println((buffer[0] & 0xFF) + " " + len);
                byte[] decrypted = cypherMode.decrypt(buffer, len);
                int paddedBytes = decrypted[len - 1 - ((this.mode == Modes.RD) ? 16 : (this.mode == Modes.RDH) ? 32 : 0)] & 0xFF;
                //System.out.println("in decrypt paddedBytes = " + paddedBytes);
                byte[] tmp = new byte[data.length + len - paddedBytes - ((this.mode == Modes.RD) ? 16 : (this.mode == Modes.RDH) ? 32 : 0)];
                System.arraycopy(data, 0, tmp, 0, data.length);
                System.arraycopy(decrypted, 0, tmp, data.length, len - paddedBytes -
                        ((this.mode == Modes.RD) ? 16 : (this.mode == Modes.RDH) ? 32 : 0));
                data = tmp;
                decryptProgress += (1f/maxProgress) * 100;
            }
            //isProgressBarEnd = true;
        }
        catch  (IOException e) {
            e.printStackTrace();
        }
        return data;
    }
}
