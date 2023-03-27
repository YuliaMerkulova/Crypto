package des;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static des.MainClass.*;

public class DesKeyExpansion implements KeyExpansion{
    public byte[][] keyExpansion(byte[] key) {
        byte[] resKey;
        byte[][] roundKeys = new byte[16][6];
        resKey = permutationBits(key, forKeyExpansion);
//        System.out.println("leftKey " + Integer.toBinaryString(resKey[0] & 0xff) +" " + Integer.toBinaryString(resKey[1]& 0xff)
//                + " "+Integer.toBinaryString(resKey[2]& 0xff) + " " + Integer.toBinaryString(resKey[3]& 0xff) + " " + Integer.toBinaryString(resKey[4]& 0xff)
//                + " " + Integer.toBinaryString(resKey[5]& 0xff) + " " + Integer.toBinaryString(resKey[6]& 0xff));
        //System.out.println("in key exp reskey: " + resKey.length);
        byte[] leftHalf = getBits(resKey, 0, 28);
//        System.out.println("leftKey " + Integer.toBinaryString(leftHalf[0] & 0xff) +" " + Integer.toBinaryString(leftHalf[1]& 0xff)
//                + " "+Integer.toBinaryString(leftHalf[2]& 0xff) + " " + Integer.toBinaryString(leftHalf[3]& 0xff));
        //System.out.println("in key exp left: " + leftHalf.length);
        byte[] rightHalf = getBits(resKey, 28, 28);
//        System.out.println("rightKey " + Integer.toBinaryString(rightHalf[0] & 0xff) +" " + Integer.toBinaryString(rightHalf[1]& 0xff)
//                + " "+Integer.toBinaryString(rightHalf[2]& 0xff) + " " + Integer.toBinaryString(rightHalf[3]& 0xff));
        for(int i = 0; i < 16; i++)
        {
            if (i == 0 || i == 1 || i == 15) {
                leftHalf = shiftArray(leftHalf, 1, i);
//                System.out.println("leftKeyAfterShift " + Integer.toBinaryString(leftHalf[0] & 0xff) +" " + Integer.toBinaryString(leftHalf[1]& 0xff)
//                        + " "+Integer.toBinaryString(leftHalf[2]& 0xff) + " " + Integer.toBinaryString(leftHalf[3]& 0xff));
                rightHalf = shiftArray(rightHalf, 1, i);
            } else {
                leftHalf = shiftArray(leftHalf, 2, i);
                rightHalf = shiftArray(rightHalf, 2, i);
            }
            roundKeys[i] = getKey(leftHalf, rightHalf, PC2);
        }
        return roundKeys;
    }
    public byte[] shiftArray(byte[] array, int shift, int r){
//        System.out.println("arrayForShift " + Integer.toBinaryString(array[0] & 0xff) +" " + Integer.toBinaryString(array[1]& 0xff)
//                + " "+Integer.toBinaryString(array[2]& 0xff) + " " + Integer.toBinaryString(array[3]& 0xff));
        BigInteger bigInt = new BigInteger(array);
        int shiftInt = bigInt.intValue();
        if (r == 0) shiftInt = shiftInt >>> 4;
//        shiftInt = shiftInt >>> 4;
        shiftInt = ((shiftInt << shift) & 268435455) | (shiftInt >>> (28 - shift));
//        shiftInt = ((shiftInt << shift) | (shiftInt >>> (28 - shift)));
        ByteBuffer buf = ByteBuffer.allocate(4);
        buf.putInt(shiftInt);
        return buf.array();
    }
    public byte[] getKey(byte[] left, byte[] right, int[] permute){
        byte[] resKey = new byte[6];
        for (int i = 0; i < permute.length; i++){
            int pos = permute[i] - 1;
            int bit;
            if (pos <= 27)
                bit = getBitFromArray(left, pos + 4);
            else bit = getBitFromArray(right, pos - 28 + 4);
            setBitIntoArray(resKey, i, bit);
        }
        return resKey;
    }
}
