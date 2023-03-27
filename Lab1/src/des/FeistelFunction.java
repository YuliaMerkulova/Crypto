package des;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.stream.IntStream;

import static des.MainClass.*;

public
 class FeistelFunction implements Cryption {
    KeyExpansion expansion;
    Encrypt encrypt;
    byte[][] roundKeys;

    public FeistelFunction(KeyExpansion expansion, Encrypt encrypt) {
        this.expansion = expansion;
        this.encrypt = encrypt;
    }

    @Override
    public byte[] encrypt(byte[] array) throws MyException{
        if (roundKeys == null)
            throw new MyException("Не настроены ключи!");
//        System.out.println("starttext: " + Integer.toBinaryString(array[0] & 0xff) +" " + Integer.toBinaryString(array[1]& 0xff)
//                + " "+Integer.toBinaryString(array[2]& 0xff) + " " + Integer.toBinaryString(array[3]& 0xff) + " "
//        + Integer.toBinaryString(array[4]& 0xff) +  " "+Integer.toBinaryString(array[5]& 0xff) +
//                " "+Integer.toBinaryString(array[6]& 0xff)+ " "+Integer.toBinaryString(array[7]& 0xff)) ;
        array = permutationBits(array, IP);

//        System.out.println("after first perm: " + Integer.toBinaryString(array[0] & 0xff) +" " + Integer.toBinaryString(array[1]& 0xff)
//                + " "+Integer.toBinaryString(array[2]& 0xff) + " " + Integer.toBinaryString(array[3]& 0xff) + " "
//                + Integer.toBinaryString(array[4]& 0xff) +  " "+Integer.toBinaryString(array[5]& 0xff) +
//                " "+Integer.toBinaryString(array[6]& 0xff)+ " "+Integer.toBinaryString(array[7]& 0xff)) ;
        byte[] prevLeft = getBits(array, 0, 32);
//        System.out.println("left: " + Integer.toBinaryString(prevLeft[0] & 0xff) +" " + Integer.toBinaryString(prevLeft[1]& 0xff)
//                + " "+Integer.toBinaryString(prevLeft[2]& 0xff) + " " + Integer.toBinaryString(prevLeft[3]& 0xff));

        byte[] prevRight = getBits(array, 32, 32);

//        System.out.println("right: " + Integer.toBinaryString(prevRight[0] & 0xff) +" " + Integer.toBinaryString(prevRight[1]& 0xff)
//                + " "+Integer.toBinaryString(prevRight[2]& 0xff) + " " + Integer.toBinaryString(prevRight[3]& 0xff));
        byte[] left = new byte[4];
        byte[] right = new byte[4];
        for (int i = 0; i < 16; i++){
            right = encrypt.encryptBlock(prevRight, roundKeys[i]);
//            System.out.println("roundKey " + Integer.toBinaryString(roundKeys[i][0] & 0xff) +" " + Integer.toBinaryString(roundKeys[i][1]& 0xff)
//                    + " "+Integer.toBinaryString(roundKeys[i][2]& 0xff) + " " + Integer.toBinaryString(roundKeys[i][3]& 0xff) +
//                     " " + Integer.toBinaryString(roundKeys[i][4]& 0xff) +  " " + Integer.toBinaryString(roundKeys[i][5]& 0xff) + " "
//                    );
            //System.out.println("beforeXOR: " + Integer.toBinaryString(prevLeft[0] & 0xff) +" " + Integer.toBinaryString(prevLeft[1]& 0xff)
            //        + " "+Integer.toBinaryString(prevLeft[2]& 0xff) + " " + Integer.toBinaryString(prevLeft[3]& 0xff));

            for (int j = 0; j < 4; j++) {
                right[j] = (byte) (right[j] ^ prevLeft[j]);
            }
            //System.out.println("afterXOR: " + Integer.toBinaryString(right[0] & 0xff) +" " + Integer.toBinaryString(right[1]& 0xff)
            //        + " "+Integer.toBinaryString(right[2]& 0xff) + " " + Integer.toBinaryString(right[3]& 0xff));
            //if (i != 15){//функция ^ левая переходят в правую
            prevLeft = prevRight;
            left = prevRight;
            prevRight = right;

//            System.out.println("afterRIGHT: " + Integer.toBinaryString(right[0] & 0xff) +" " + Integer.toBinaryString(right[1]& 0xff)
//                    + " "+Integer.toBinaryString(right[2]& 0xff) + " " + Integer.toBinaryString(right[3]& 0xff));

//            System.out.println("afterLEFT: " + Integer.toBinaryString(left[0] & 0xff) +" " + Integer.toBinaryString(left[1]& 0xff)
//                    + " "+Integer.toBinaryString(left[2]& 0xff) + " " + Integer.toBinaryString(left[3]& 0xff));

//                prevRight = right;
//                prevLeft = left;
            //}
            //else { //на последнем блоке не меняются местами, функция ^ левая остается в левой
            //    left = right;
            //    right = prevRight;
            //}
        }
        byte[] resArray = new byte[8];
        //склеиваем в один блок
        for (byte i = 0; i < 4; i++) {
            resArray[i] = left[i];
            resArray[i+4] = right[i];
        }
        resArray = permutationBits(resArray, IIP);
        return resArray;
    }

    @Override
    public byte[] decrypt(byte[] array) throws MyException {
        if (roundKeys == null)
            throw new MyException("Не настроены ключи!");
        //System.out.println("after permut" + Integer.toHexString((new BigInteger(array)).intValue()));
        array = permutationBits(array, IP);
        //System.out.println("after permut" + Integer.toHexString((new BigInteger(array)).intValue()));
        byte[] prevLeft = getBits(array, 0, 32);
        byte[] prevRight = getBits(array, 32, 32);
        byte[] left = new byte[4];
        byte[] right = new byte[4];
        for (int i = 0; i < 16; i++){
            left = encrypt.encryptBlock(prevLeft, roundKeys[15 - i]); //используем ключи в обратном порядке
            for (int j = 0; j < 4; j++){
                left[j] = (byte) (left[j] ^ prevRight[j]);
            }
            //if (i != 15){//функция ^ левая переходят в левую
            prevRight = prevLeft;
            right = prevLeft;
            prevLeft = left;
            //}
            //else { //на последнем блоке не меняются местами, функция ^ левая остается в левой
            //    left = prevLeft;
            //}
        }
        byte[] resArray = new byte[8];
        //склеиваем в один блок
        for (byte i = 0; i < 4; i++) {
            resArray[i] = left[i];
            resArray[i+4] = right[i];
        }
        resArray = permutationBits(resArray, IIP);

        return resArray;
    }

    @Override
    public void setRoundKeys(byte[] key) {
        roundKeys = expansion.keyExpansion(key);
    }
}
