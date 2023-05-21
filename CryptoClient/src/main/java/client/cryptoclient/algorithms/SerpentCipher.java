package client.cryptoclient.algorithms;

import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static client.cryptoclient.algorithms.SerpentHelper.*;
@Slf4j
public class SerpentCipher {


    private int[] key;

    private byte[][] K = new byte[33][16];


    public SerpentCipher(int[] key) {
        this.key = key.clone();
        getPuddedKey();
        generateRoundKeys(generatePreKeys());
    }

    public static byte[] permutationBits(byte[] array, int[] pPermutationBlock){
        byte[] resArray = new byte[pPermutationBlock.length / 8];
        for (int i = 0; i < pPermutationBlock.length; i++){
            int pos = pPermutationBlock[i];
            int bit = getBitFromArray(array, pos);
            setBitIntoArray(resArray, i, bit);
        }
        return resArray;
    }
    public static int getBitFromArray(byte[] array, int pos){
        int bytePos = pos / 8;
        int bitPos = pos % 8;
        return ((array[bytePos] >>> (8 - bitPos - 1)) & 0b1);
    }
    public static void setBitIntoArray(byte[] array, int pos, int bit){
        int bytePos = pos / 8;
        int bitPos = pos % 8;
        byte oldByte = array[bytePos];
        oldByte |= bit << (8 - bitPos - 1);
        array[bytePos] = oldByte;
    }
    public void getPuddedKey() {
        int[] pudded = new int[8];
        int len = key.length;
        System.arraycopy(key, 0, pudded, 0, len);
        if (len < 8) {
            pudded[len] = 1 << 31;
            for (int i = len + 1; i < 8; i++) {
                pudded[i] = 0;
            }
        }
        key = pudded;
    }

    public int[] generatePreKeys() {
        int goldenRatio = 0x9e3779b9;
        int[] w = new int[132];
        System.arraycopy(key, 0, w, 0, 8);
        for (int i = 8; i < 132; i++) {
            int temp = w[i - 8] ^ w[i - 5] ^ w[i - 3] ^ w[i - 1] ^ goldenRatio ^ (i - 8);
            w[i] = (temp << 11) | (temp >>> 21);
        }
        return w;
    }

    public void generateRoundKeys(int w[]) {
        int j = 3;
        byte[][] keys = new byte[132][4];
        for (int i = 0; i < 33; i++) {
            for (int k = i * 4; k < i * 4 + 4; k++) {
                var key_left = permutationBits(new byte[] {(byte) (w[k] >>> 24 & 0xff), (byte) (w[k] >>> 16 & 0x00ff)}, sBoxTable[j]);
                var key_right = permutationBits(new byte[] {(byte) (w[k] >>> 8 & 0x0000ff), (byte) (w[k] & 0x000000ff)}, sBoxTable[j]);
                keys[k][0] = (key_left[0]);
                keys[k][1] = (key_left[1]);
                keys[k][2] = (key_right[0]);
                keys[k][3] = (key_right[1]);
                j--;
                if (j == -1){
                    j = 7;
                }
            }
        }
        for (int i = 0; i < 33; i++) {
            for (int t = 0; t < 16; t++) {
                K[i][t] = keys[i * 4 + t / 4][t % 4];
            }
        }

    }

    public byte[] decrypt(byte[] input) {
        input = permutationBits(input, IP);
        for (int i = 31; i >= 0; i--) {
            if (i == 31){
                for (int j = 0; j < 16; j++){
                    input[j] = (byte) (K[i + 1][j] ^ input[j]);
                }
            }
            else {
                inverseLinearTransform(input);
            }

            for (int j = 0; j < 16; j++){
                byte tempL = (byte) (input[j] & 0x0f);
                byte tempR = (byte) ((input[j] & 0xf0) >> 4);
                input[j] = (byte) ((replaceWithInverseSBox(tempL, i % 8) << 4) | replaceWithInverseSBox(tempR, i % 8) & 0xff);
            }
            for (int j = 0; j < 16; j++){
                input[j] = (byte) (K[i][j] ^ input[j]);
            }
        }

        input = permutationBits(input, FP);
        return input;
    }

    public byte[] encryptFile(byte[] input){
        int len =  input.length;
        ExecutorService service = Executors.newFixedThreadPool(4);
        List<Future<byte[]>> futureBlocks = new ArrayList<Future<byte[]>>();
        for(int i = 0; i < input.length - 16; i+=16)
        {
            byte[] block = Arrays.copyOfRange(input, i, i + 16);
            futureBlocks.add(service.submit(() -> encrypt(block)));
            //byte[] encrypted = encrypt(block);
            //System.arraycopy(encrypted, 0, input, i, i + 16);
        }
        //System.out.println("Finish");
        byte lastByte =  (byte) (16 - (len % 16));
        byte[] block = new byte[16];
        System.arraycopy(input, len - len % 16, block, 0, len % 16);
        //System.out.println("Start2");
        for (int i = len % 16; i < 16; i++){
            block[i] = lastByte;
        }
        futureBlocks.add(service.submit(() -> encrypt(block)));

        byte[] encrypted = new byte[len + (16 - len % 16)];
        //System.out.println("Start3");
        for(int i = 0; i < encrypted.length / 16; i += 1) {
            try {
                byte[] encBlock = futureBlocks.get(i).get();
                System.arraycopy(encBlock, 0, encrypted, i * 16, 16);
            } catch (InterruptedException | ExecutionException e){
                log.error("Something wrong  in cipher");
            }
        }
        System.out.println("End4");
        return encrypted;
    }

    public byte[] decryptData(byte[] input) throws ExecutionException, InterruptedException {
        int len =  input.length;
        ExecutorService service = Executors.newFixedThreadPool(4);
        List<Future<byte[]>> futureBlocks = new ArrayList<Future<byte[]>>();
        for(int i = 0; i < input.length - 16; i+=16)
        {
            byte[] block = Arrays.copyOfRange(input, i, i + 16);
            futureBlocks.add(service.submit(() -> decrypt(block)));
            //byte[] encrypted = encrypt(block);
            //System.arraycopy(encrypted, 0, input, i, i + 16);
        }
        byte[] lastBlock = new byte[16];
        System.arraycopy(input, len - 16, lastBlock, 0, 16);
        lastBlock = decrypt(lastBlock);
        int num = lastBlock[lastBlock.length - 1] & 0xff;
        byte[] decryptData = new byte[len - num];
        System.arraycopy(lastBlock, 0, decryptData, decryptData.length - 16 + num, lastBlock.length - num);
        for(int i = 0; i < len / 16 - 1; i++){
            byte[] block = futureBlocks.get(i).get();
            System.arraycopy(block, 0, decryptData, i * 16, 16);
        }
        return decryptData;
    }

    public byte[] encrypt(byte[] input) {//128 на вход
        input = permutationBits(input, IP);
        for (int i = 0; i < 32; i++){//rounds
            for (int j = 0; j < 16; j++){
                input[j] = (byte) (K[i][j] ^ input[j]);
            }
            //TODO parallelize
            for (int j = 0; j < 16; j++){
                byte tempL = (byte) (input[j] & 0x0f);
                byte tempR = (byte) ((input[j] & 0xf0) >> 4);
                input[j] = (byte) ((replaceWithSBox(tempL, i % 8) << 4) | replaceWithSBox(tempR, i % 8) & 0xff);
            }
            if (i != 31){
                linearTransform(input);
            } else {
                for (int j = 0; j < 16; j++){
                    input[j] = (byte) (K[i + 1][j] ^ input[j]);
                }
            }
        }
        input = permutationBits(input, FP);
        return input;
    }

    public byte replaceWithSBox(byte b, int i){
        return (byte) sBoxTable[i][b];
    }

    public byte replaceWithInverseSBox(byte b, int i){
        return (byte) sBoxInverseTable[i][b];
    }

    public void linearTransform(byte[] input) {
        int[] x = new int[4];
        for (int i = 0; i < 4; i++) {
            x[i] = (input[i*4] & 0xff)<< 24 | (input[i*4 + 1]& 0xff) << 16 | (input[i*4 + 2] & 0xff) << 8 | (input[i*4 + 3] & 0xff);
        }
        x[0] = ((x[0] << 13) | (x[0] >>> (32 - 13)));
        x[2] = x[2] << 3 | x[2] >>> 29;
        x[1] = x[1] ^ x[0] ^ x[2];
        x[3] = x[3] ^ x[2] ^ (x[0] << 3);
        x[1] = x[1] << 1 | x[1] >>> 31;
        x[3] = x[3] << 7 | x[3] >>> 25;
        x[0] = x[0] ^ x[1] ^ x[3];
        x[2] = x[2] ^ x[3] ^ (x[1] << 7);
        x[0] = x[0] << 5 | x[0] >>> 27;
        x[2] = x[2] << 22 | x[2] >>> 10;

        ByteBuffer buffer = ByteBuffer.allocate(16);
        IntBuffer intBuf = IntBuffer.wrap(x);
        buffer.asIntBuffer().put(intBuf);
        System.arraycopy(buffer.array(), 0, input, 0, 16);
    }

    public void inverseLinearTransform(byte[] input) {
        int[] x = new int[4];
        for (int i = 0; i < 4; i++) {
            x[i] = (input[i * 4] & 0xff) << 24 | (input[i * 4 + 1] & 0xff) << 16 | (input[i * 4 + 2] & 0xff) << 8 | (input[i * 4 + 3] & 0xff);
        }
        x[2] = x[2] >>> 22 | x[2] << 10;
        x[0] = x[0] >>> 5 | x[0] << 27;
        x[2] = x[2] ^ x[3] ^ (x[1] << 7);
        x[0] = x[0] ^ x[1] ^ x[3];
        x[3] = x[3] >>> 7 | x[3] << 25;
        x[1] = x[1] >>> 1 | x[1] << 31;
        x[3] = x[3] ^ x[2] ^ (x[0] << 3);
        x[1] = x[1] ^ x[0] ^ x[2];
        x[2] = x[2] >>> 3 | x[2] << 29;
        x[0] = x[0] >>> 13 | x[0] << 19;
        ByteBuffer buffer = ByteBuffer.allocate(16);
        IntBuffer intBuf = IntBuffer.wrap(x);
        buffer.asIntBuffer().put(intBuf);
        System.arraycopy(buffer.array(), 0, input, 0, 16);
    }
}

