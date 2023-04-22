package des;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;


public class SimmetricalCipher {

    private final Cryption algo;
    private byte[] key;
    ModesCipher mode;
    Mode concreteMode;
    private byte[] initVector;

    protected SimmetricalCipher(byte[] key, ModesCipher mode, byte[] vector, Cryption algo){
        this.key = key;
        this.mode = mode;
        this.initVector = vector;
        this.algo = algo;
        this.algo.setRoundKeys(this.key);
        this.concreteMode = returnConcreteMode();
    }
    private Mode returnConcreteMode(){
        switch (mode) {
            case ECB :
                return new ModeECB(algo);
            case CBC:
                return new ModeCBC(algo, initVector);
            case CFB:
                return new ModeCFB(algo, initVector);
            case OFB:
                return new ModeOFB(algo, initVector);
            case CTR:
                return new ModeCTR(algo, initVector);
            case RD:
                return new ModeRD(algo, initVector);
            case RDH:
                return new ModeRDH(algo, initVector);
        }
        return new ModeECB(algo);
    }

    public void encryptData(String file, String outFile) {
        // TODO: add mode matcher
        concreteMode.reset();
        byte[] buffer = new byte[80000];
        int len;
        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            int length =  (this.mode == des.ModesCipher.RD) ? 79984 : (this.mode == des.ModesCipher.RDH) ? 79976 : 79992;
            while ((len = fileInputStream.read(buffer, 0, length)) > 0) {
                int last = len % 8;
                Arrays.fill(buffer, len, len + 8 - last, (byte) (8 - last));
                fileOutputStream.write(concreteMode.encrypt(buffer, len + 8 - last), 0 , len + 8 - last);
            }
        }
        catch  (IOException e){
            System.out.println(e.getMessage());
        }


    }
    public void decryptData(String file, String outFile) {
        concreteMode.reset();
        byte[] buffer = new byte[80000];
        int len;
        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            while ((len = fileInputStream.read(buffer)) > 0) {
                System.out.println("dec");
                byte[] newBuf = concreteMode.decrypt(buffer, len);
                int last = newBuf[len - 1];
                fileOutputStream.write(newBuf, 0 , len - last);
            }
        }
        catch  (IOException e){

            System.out.println(e.getMessage());
        }

    }
}
