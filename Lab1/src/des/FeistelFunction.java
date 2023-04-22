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
        array = permutationBits(array, IP);
        byte[] prevLeft = getBits(array, 0, 32);
        byte[] prevRight = getBits(array, 32, 32);
        byte[] left = new byte[4];
        byte[] right = new byte[4];
        for (int i = 0; i < 16; i++){
            right = encrypt.encryptBlock(prevRight, roundKeys[i]);
            for (int j = 0; j < 4; j++) {
                right[j] = (byte) (right[j] ^ prevLeft[j]);
            }
            prevLeft = prevRight;
            left = prevRight;
            prevRight = right;
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
        array = permutationBits(array, IP);
        byte[] prevLeft = getBits(array, 0, 32);
        byte[] prevRight = getBits(array, 32, 32);
        byte[] left = new byte[4];
        byte[] right = new byte[4];
        for (int i = 0; i < 16; i++){
            left = encrypt.encryptBlock(prevLeft, roundKeys[15 - i]); //используем ключи в обратном порядке
            for (int j = 0; j < 4; j++){
                left[j] = (byte) (left[j] ^ prevRight[j]);
            }
            prevRight = prevLeft;
            right = prevLeft;
            prevLeft = left;
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
