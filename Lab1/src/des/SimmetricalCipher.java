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
//        byte[] arrayData = new byte[8];
//        arrayData[0] = (byte) 0b0001_0010;
//        arrayData[1] = (byte) 0b0011_0100;
//        arrayData[2] = (byte) 0b0101_0110;
//        arrayData[3] = (byte) 0b1010_1011;
//        arrayData[4] = (byte) 0b1100_1101;
//        arrayData[5] = (byte) 0b0001_0011;
//        arrayData[6] = (byte) 0b0010_0101;
//        arrayData[7] = (byte) 0b0011_0110;
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
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> encryptedBlocksFutures = new LinkedList<>();
        byte[] buffer = new byte[8];

        int lastBlockLen = 0;
        int len;
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while ((len = fileInputStream.read(buffer)) > 0) {
                System.out.println("read : " + new String(buffer, StandardCharsets.UTF_8));
                if (len != 8) {
                    for (int i = 7; i >= len; i--) {
                        buffer[i] = (byte) (8 - len);
                    }
                }
                byte[] newBuf = Arrays.copyOfRange(buffer, 0, buffer.length);

                encryptedBlocksFutures.add(service.submit(() -> algo.encrypt(newBuf)));

                lastBlockLen = len;
            }
            if (lastBlockLen == 8) {
                Arrays.fill(buffer, (byte) 8);
                byte[] newBuf = Arrays.copyOfRange(buffer, 0, buffer.length);
                encryptedBlocksFutures.add(service.submit(() -> algo.encrypt(newBuf)));
            }
            service.shutdown();
        }
        catch (IOException e) {
            System.out.println("gfdsdsdvbn");
        }
        try(FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            for (var futureBufToWrite : encryptedBlocksFutures) {
                fileOutputStream.write(futureBufToWrite.get());
            }
        }
        catch (IOException e) {
            System.out.println("gfdsdsdvbn");
        } catch (ExecutionException | InterruptedException e) {
            System.out.println("gfdsdsdvb23n");
        }

    }
    public void decryptData(String file, String outFile) {
        // TODO: add mode matcher
        byte[] buffer = new byte[8];
        System.out.println("Start decrypt");
        int processors = Runtime.getRuntime().availableProcessors();
        ExecutorService service = Executors.newFixedThreadPool(processors);
        List<Future<byte[]>> decryptedBlocksFutures = new LinkedList<>();

        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            while (fileInputStream.read(buffer) > 0) {

                byte[] b1 = Arrays.copyOfRange(buffer, 0, buffer.length);
                decryptedBlocksFutures.add(service.submit(()->algo.decrypt(b1)));
            }
            service.shutdown();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Start write");
        try (FileOutputStream fileOutputStream = new FileOutputStream(outFile)) {
            int futuresSize = decryptedBlocksFutures.size();
            int cnt = 0;
            //System.out.println("size : " + futuresSize);
            for (var futureBufToWrite : decryptedBlocksFutures) {
                byte[] buf = futureBufToWrite.get();
                System.out.println("Block cnt " + cnt);
                if (++cnt == futuresSize) {
                    System.out.println("ravno");
                    int last = buf[7];
                    System.out.println(last);
                    if (last == 8) {
                        break;
                    }
                    System.out.println("start copy");
                    buf = Arrays.copyOfRange(buf, 0, 8 - last);
                    System.out.println("end copy");
                }
                System.out.println("write");
                fileOutputStream.write(buf);
                System.out.println("end write");
            }
        }

        catch (IOException | InterruptedException | ExecutionException e) {
            //balbla
        }
    }
}
