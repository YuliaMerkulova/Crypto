package des;

import static des.MainClass.*;

public class DesEncrypt implements Encrypt{
    @Override
    public byte[] encryptBlock(byte[] array, byte[] roundKey) {
        byte[] arrAfterExpansion = permutationBits(array, E);//расширение E до 6 byte
        for(int i = 0; i < arrAfterExpansion.length; i++){//ксор с ключом
            arrAfterExpansion[i] ^= roundKey[i];
        }
        byte[] arrAfterSbox = new byte[4];
        int count = 0;
        byte val = 0;
        //замена Sbox
        for(byte b: arrAfterExpansion){
            for(int i = 0; i < 8; i++) {
                val = (byte) ((val << 1) | ((b & 0b1000_0000) >>> 7));
                count++;
                if (count % 6 == 0) {
                    arrAfterSbox[(count / 6 - 1) / 2] = (byte) ((arrAfterSbox[(count / 6 - 1) / 2] << 4) | (replaceWithSbox(val, S[count / 6 - 1]) & 0xf));
                    val = 0;
                }
                b = (byte) (b << 1);
            }
        }
        //перестановка P
        arrAfterSbox = permutationBits(arrAfterSbox, pPermutationBlock);
        return arrAfterSbox;
    }
}
