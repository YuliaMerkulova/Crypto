package des;

import java.math.BigInteger;
import java.nio.ByteBuffer;

import static des.MainClass.*;

public class DesKeyExpansion implements KeyExpansion{
    public byte[][] keyExpansion(byte[] key) {
        byte[] resKey;
        byte[][] roundKeys = new byte[16][6];
        resKey = permutationBits(key, forKeyExpansion);
        byte[] leftHalf = getBits(resKey, 0, 28);
        byte[] rightHalf = getBits(resKey, 29, 28);
        for(int i = 0; i < 16; i++)
        {
            if (i == 0 || i == 1 || i == 15){
                System.out.println();
                System.out.println("lbeforeshift");
                for (byte b : leftHalf) {
                    System.out.print(String.format("%x", b));
                }
                System.out.println();
                leftHalf = shiftArray(leftHalf, 1, i);
                System.out.println("laftershift");
                for (byte b : leftHalf) {
                    System.out.print(String.format("%x", b));
                }
                System.out.println();
                System.out.println("rbeforeshift");
                for (byte b : rightHalf) {
                    System.out.print(String.format("%x", b));
                }
                System.out.println();
                rightHalf = shiftArray(rightHalf, 1, i);
                System.out.println("raftershift");
                for (byte b : rightHalf) {
                    System.out.print(String.format("%x", b));
                }
                System.out.println();
            } else{
                System.out.println();
                System.out.println("lbeforeshift");
                for (byte b : leftHalf) {
                    System.out.print(String.format("%x", b));
                }
                System.out.println();
                leftHalf = shiftArray(leftHalf, 2, i);
                System.out.println("laftershift");
                for (byte b : leftHalf) {
                    System.out.print(String.format("%x", b));
                }
                System.out.println();
                System.out.println("rbeforeshift");
                for (byte b : rightHalf) {
                    System.out.print(String.format("%x", b));
                }
                System.out.println();
                rightHalf = shiftArray(rightHalf, 2, i);
                System.out.println("raftershift:");
                for (byte b : rightHalf) {
                    System.out.print(String.format("%x", b));
                }
            }

            roundKeys[i] = getKey(leftHalf, rightHalf, PC2);
        }
        return roundKeys;
    }
    public byte[] shiftArray(byte[] array, int shift, int r){
        //System.out.println("here0");
        BigInteger bigInt = new BigInteger(array);
        //System.out.println("here1" + bigInt.intValue());
        int shiftInt = bigInt.intValue();
        if (r == 0) shiftInt = shiftInt >>> 4;
       //System.out.println("shift" + String.format("%x", shiftInt));
        //System.out.println("here11");
        shiftInt = ((shiftInt << shift) | (shiftInt >>> (32 - shift)));
        //System.out.println("here2");
        ByteBuffer buf = ByteBuffer.allocate(4);
        //System.out.println("here3");
        buf.putInt(shiftInt);
        return buf.array();
    }
    public byte[] getKey(byte[] left, byte[] right, int[] permute){
        byte[] resKey = new byte[6];
        for (int i = 0; i < permute.length; i++){
            int pos = permute[i] - 1;
            int bit;
            if (pos <= 27)
                bit = getBitFromArray(left, pos);
            else bit = getBitFromArray(right, pos - 27);
            setBitIntoArray(resKey, i, bit);
        }
        return resKey;
    }
}
