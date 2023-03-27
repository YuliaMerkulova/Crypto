package des;

import static des.MainClass.*;

public class DesEncrypt implements Encrypt{
    @Override
    public byte[] encryptBlock(byte[] array, byte[] roundKey) {
        //System.out.println("in encryptBlock: " + Integer.toBinaryString(array[0] & 0xff) +" " + Integer.toBinaryString(array[1]& 0xff)
        //        + " "+Integer.toBinaryString(array[2]& 0xff) + " " + Integer.toBinaryString(array[3]& 0xff));
        byte[] arrAfterExpansion = permutationBits(array, E);//расширение E 6 byte res
        //System.out.println("afterPermute: " + Integer.toBinaryString(arrAfterExpansion[0] & 0xff) +" " + Integer.toBinaryString(arrAfterExpansion[1]& 0xff)
        //        + " "+Integer.toBinaryString(arrAfterExpansion[2]& 0xff) + " " + Integer.toBinaryString(arrAfterExpansion[3]& 0xff)
        //+ " " + Integer.toBinaryString(arrAfterExpansion[4]& 0xff) + " " +Integer.toBinaryString(arrAfterExpansion[5]& 0xff));
        //System.out.println("arr len1 = " + arrAfterExpansion.length + " l 2 = "+ roundKey.length);

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
                    //System.out.println("before S: " + Integer.toBinaryString(val& 0b0011_1111));
                    arrAfterSbox[(count / 6 - 1) / 2] = (byte) ((arrAfterSbox[(count / 6 - 1) / 2] << 4) | (replaceWithSbox(val, S[count / 6 - 1]) & 0xf));
                    val = 0;
                }
                b = (byte) (b << 1);
                //System.out.println("byteafter<<: " + Integer.toBinaryString(b& 0b1111_1111));
            }
        }
        //System.out.println("afterSbox: " + Integer.toBinaryString(arrAfterSbox[0] & 0xff) +" " + Integer.toBinaryString(arrAfterSbox[1]& 0xff)
        //        + " "+Integer.toBinaryString(arrAfterSbox[2]& 0xff) + " " + Integer.toBinaryString(arrAfterSbox[3]& 0xff));

        //перестановка P
        arrAfterSbox = permutationBits(arrAfterSbox, pPermutationBlock);
        //System.out.println("afterSboxPermute: " + Integer.toBinaryString(arrAfterSbox[0] & 0xff) +" " + Integer.toBinaryString(arrAfterSbox[1]& 0xff)
        //        + " "+Integer.toBinaryString(arrAfterSbox[2]& 0xff) + " " + Integer.toBinaryString(arrAfterSbox[3]& 0xff));

        return arrAfterSbox;
    }
}
