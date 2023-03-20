package des;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.Key;
import java.util.Arrays;

public class MainClass {
    static final int[] pPermutationBlock  = new int[] {16, 7, 20, 21, 29, 12, 28, 17,
            1, 15, 23, 26, 5, 18, 31, 10,
            2, 8, 24, 14, 32, 27, 3, 9,
            19, 13, 30, 6, 22, 11, 4, 25};
    static final int[] S1 = {
            14,  4, 13,  1,  2, 15, 11,  8,  3, 10,  6, 12,  5,  9,  0,  7,
            0, 15,  7,  4, 14,  2, 13,  1, 10,  6, 12, 11,  9,  5,  3,  8,
            4,  1, 14,  8, 13,  6,  2, 11, 15, 12,  9,  7,  3, 10,  5,  0,
            15, 12,  8,  2,  4,  9,  1,  7,  5, 11,  3, 14, 10,  0,  6, 13
    };
    static final int[] S2 = {
            15,  1,  8, 14,  6, 11,  3,  4,  9,  7,  2, 13, 12,  0,  5, 10,
            3, 13,  4,  7, 15,  2,  8, 14, 12,  0,  1, 10,  6,  9, 11,  5,
            0, 14,  7, 11, 10,  4, 13,  1,  5,  8, 12,  6,  9,  3,  2, 15,
            13,  8, 10,  1,  3, 15,  4,  2, 11,  6,  7, 12,  0,  5, 14,  9
    };
    static final int[] S3 = {
            10,  0,  9, 14,  6,  3, 15,  5,  1, 13, 12,  7, 11,  4,  2,  8,
            13,  7,  0,  9,  3,  4,  6, 10,  2,  8,  5, 14, 12, 11, 15,  1,
            13,  6,  4,  9,  8, 15,  3,  0, 11,  1,  2, 12,  5, 10, 14,  7,
            1, 10, 13,  0,  6,  9,  8,  7,  4, 15, 14,  3, 11,  5,  2, 12
    };
    static final int[] S4 = {
            7, 13, 14,  3,  0,  6,  9, 10,  1,  2,  8,  5, 11, 12,  4, 15,
            13,  8, 11,  5,  6, 15,  0,  3,  4,  7,  2, 12,  1, 10, 14,  9,
            10,  6,  9,  0, 12, 11,  7, 13, 15,  1,  3, 14,  5,  2,  8,  4,
            3, 15,  0,  6, 10,  1, 13,  8,  9,  4,  5, 11, 12,  7,  2, 14
    };
    static final int[] S5 = {
            2, 12,  4,  1,  7, 10, 11,  6,  8,  5,  3, 15, 13,  0, 14,  9,
            14, 11,  2, 12,  4,  7, 13,  1,  5,  0, 15, 10,  3,  9,  8,  6,
            4,  2,  1, 11, 10, 13,  7,  8, 15,  9, 12,  5,  6,  3,  0, 14,
            11,  8, 12,  7,  1, 14,  2, 13,  6, 15,  0,  9, 10,  4,  5,  3
    };
    static final int[] S6 = {
            12,  1, 10, 15,  9,  2,  6,  8,  0, 13,  3,  4, 14,  7,  5, 11,
            10, 15,  4,  2,  7, 12,  9,  5,  6,  1, 13, 14,  0, 11,  3,  8,
            9, 14, 15,  5,  2,  8, 12,  3,  7,  0,  4, 10,  1, 13, 11,  6,
            4,  3,  2, 12,  9,  5, 15, 10, 11, 14,  1,  7,  6,  0,  8, 13
    };
    static final int[] S7 = {
            4, 11,  2, 14, 15,  0,  8, 13,  3, 12,  9,  7,  5, 10,  6,  1,
            13,  0, 11,  7,  4,  9,  1, 10, 14,  3,  5, 12,  2, 15,  8,  6,
            1,  4, 11, 13, 12,  3,  7, 14, 10, 15,  6,  8,  0,  5,  9,  2,
            6, 11, 13,  8,  1,  4, 10,  7,  9,  5,  0, 15, 14,  2,  3, 12
    };
    static final int[] S8 = {
            13,  2,  8,  4,  6, 15, 11,  1, 10,  9,  3, 14,  5,  0, 12,  7,
            1, 15, 13,  8, 10,  3,  7,  4, 12,  5,  6, 11,  0, 14,  9,  2,
            7, 11,  4,  1,  9, 12, 14,  2,  0,  6, 10, 13, 15,  3,  5,  8,
            2,  1, 14,  7,  4, 10,  8, 13, 15, 12,  9,  0,  3,  5,  6, 11
    };
    static final int[] forKeyExpansion = {
            57, 49, 41, 33, 25, 17,  9,
            1, 58, 50, 42, 34, 26, 18,
            10,  2, 59, 51, 43, 35, 27,
            19, 11,  3, 60, 52, 44, 36,
            63, 55, 47, 39, 31, 23, 15,
            7, 62, 54, 46, 38, 30, 22,
            14,  6, 61, 53, 45, 37, 29,
            21, 13,  5, 28, 20, 12,  4
    };
    static final int[] PC2 = {
            14, 17, 11, 24,  1,  5,
            3, 28, 15,  6, 21, 10,
            23, 19, 12,  4, 26,  8,
            16,  7, 27, 20, 13,  2,
            41, 52, 31, 37, 47, 55,
            30, 40, 51, 45, 33, 48,
            44, 49, 39, 56, 34, 53,
            46, 42, 50, 36, 29, 32
    };

    public static byte[] permutationBits(byte[] array, int[] pPermutationBlock){
        byte[] resArray = new byte[pPermutationBlock.length];
        for (int i = 0; i < pPermutationBlock.length; i++){
            int pos = pPermutationBlock[i] - 1;
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
    public static byte replaceWithSbox(byte oldByte, int[] Sbox){
        byte newByte;
        int row = (((oldByte >>> 5)& 1) << 1) | (oldByte & 1);
        int col = (oldByte >>> 1) & 0xf;
        newByte = (byte)Sbox[row * 16 + col];
        return newByte;
    }
    public static byte[] getBits(byte[] array, int startPos, int length){
        int numBytes = length / 8 + 1;
        byte[] resArray = new byte[numBytes];
        for (int i = 0; i < length; i++){
            int value = getBitFromArray(array, startPos + i);
            setBitIntoArray(resArray, i, value);
        }
        return resArray;
    }

    public static void main(String[] args){
        byte[] array = new byte[8];
        array[0] = (byte) 0b1001_0011;
        array[1] = (byte) 0b0111_1111;
        array[2] = (byte) 0b1111_1111;
        array[3] = (byte) 0b1111_1111;
        array[4] = (byte) 0b1111_1111;
        array[5] = (byte) 0b1111_1111;
        array[6] = (byte) 0b1111_1111;
        array[7] = (byte) 0b1111_1111;
        DesKeyExpansion desKeyExpansion = new DesKeyExpansion();
        desKeyExpansion.keyExpansion(array);
        //byte[] newarray = permutationBitsP(array, pPermutationBlock, 32);
        //System.out.println(Integer.toBinaryString(newarray[0] & 0xff) +" " + Integer.toBinaryString(newarray[1]& 0xff)
        //+ " "+Integer.toBinaryString(newarray[2]& 0xff) + " " + Integer.toBinaryString(newarray[3]& 0xff)) ;
        byte a = (byte) 0b0010_1110;
        //System.out.println((a % 2 == 0? "YES":"NO"));
        //System.out.println((((a >> 1) & 0xf)));
    }
}
