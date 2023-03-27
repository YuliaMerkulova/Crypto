package des;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class SimmetricalCipher {

    private final Cryption algo;
    private byte[] key;
    ModesCipher mode;
    private byte[] initVector;

    protected SimmetricalCipher(byte[] key, ModesCipher mode, byte[] vector, Cryption algo){
        this.key = key;
        this.mode = mode;
        this.initVector = vector;
        this.algo = algo;
        this.algo.setRoundKeys(this.key);
    }

    public void encryptData(String file, String outFile) {
        // TODO: add mode matcher
        byte[] arrayData = new byte[8];
        arrayData[0] = (byte) 0b0001_0010;
        arrayData[1] = (byte) 0b0011_0100;
        arrayData[2] = (byte) 0b0101_0110;
        arrayData[3] = (byte) 0b1010_1011;
        arrayData[4] = (byte) 0b1100_1101;
        arrayData[5] = (byte) 0b0001_0011;
        arrayData[6] = (byte) 0b0010_0101;
        arrayData[7] = (byte) 0b0011_0110;
//        try {

//            byte[] b1 = algo.encrypt(arrayData);
//            System.out.println("decr : " + Integer.toBinaryString(b1[0] & 0xff) +" " + Integer.toBinaryString(b1[1]& 0xff)
//                    + " "+Integer.toBinaryString(b1[2]& 0xff) + " " + Integer.toBinaryString(b1[3]& 0xff) + " "
//                    + Integer.toBinaryString(b1[4]& 0xff) +  " "+Integer.toBinaryString(b1[5]& 0xff) +
//                    " "+Integer.toBinaryString(b1[6]& 0xff)+ " "+Integer.toBinaryString(b1[7]& 0xff)) ;
//            byte[] b2 = algo.decrypt(b1);
//            System.out.println("decr : " + Integer.toBinaryString(b2[0] & 0xff) +" " + Integer.toBinaryString(b2[1]& 0xff)
//                    + " "+Integer.toBinaryString(b2[2]& 0xff) + " " + Integer.toBinaryString(b2[3]& 0xff) + " "
//                    + Integer.toBinaryString(b2[4]& 0xff) +  " "+Integer.toBinaryString(b2[5]& 0xff) +
//                    " "+Integer.toBinaryString(b2[6]& 0xff)+ " "+Integer.toBinaryString(b2[7]& 0xff)) ;
//
//
//        }
//        catch (Exception e) {
//            System.out.println("asbkmfshlkfs");
//        }

        byte[] buffer = new byte[8];
        Arrays.fill(buffer, (byte) 0);

        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            while (fileInputStream.read(buffer) > 0) {
                System.out.println("read : " + new String(buffer, StandardCharsets.UTF_8));
                byte[] b1 = algo.encrypt(buffer);
                System.out.println("enc bu = " + b1[0] + " " + b1[1]);
                System.out.println("jj" + b1);
                fileOutputStream.write(b1);
                Arrays.fill(buffer, (byte) 0);

            }
        }
        catch (IOException | MyException e) {
            System.out.println("gfdsdsdvbn");
        }

    }
    public void decryptData(String file, String outFile) {
        // TODO: add mode matcher
        byte[] buffer = new byte[8];
        Arrays.fill(buffer, (byte) 0);

        try (FileInputStream fileInputStream = new FileInputStream(file);
             FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            while (fileInputStream.read(buffer) > 0) {
                byte[] b1 = algo.decrypt(buffer);
                System.out.println("dec bu = " + buffer[0] + " " + buffer[1]);
                System.out.println("oo " + b1);
                fileOutputStream.write(b1);
                Arrays.fill(buffer, (byte) 0);
            }
        }
        catch (IOException | MyException e) {
            //balbla
        }
    }
}
