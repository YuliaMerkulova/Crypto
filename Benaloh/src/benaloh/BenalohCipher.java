package benaloh;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

import static benaloh.BenalohHelper.generatePrime;

public class BenalohCipher {

    private PrivateKey privateKey = new PrivateKey();
    private PublicKey publicKey = new PublicKey();

    public BenalohCipher(int keySize){
        generateKey(keySize);
    }

    private void generateKey(int keySize) {
        while ((publicKey.r = generatePrime(10)).compareTo(BigInteger.valueOf(256)) <= 0);

        BigInteger pMinusOne;
        System.out.println(
                publicKey.r
        );
        do { // генерация p такого, что p-1 делится на r и p-1/r взаимно просто с r
            privateKey.p = generatePrime(keySize);
            pMinusOne = privateKey.p.subtract(BigInteger.ONE);
        } while(pMinusOne.mod(publicKey.r).intValue() != 0
                || pMinusOne.divide(publicKey.r).gcd(publicKey.r).intValue() != 1);
        BigInteger qMinusOne;

        do {//генерация q такого, что q-1 и r взаимно просты
            privateKey.q = generatePrime(keySize - 1);
            qMinusOne = privateKey.q.subtract(BigInteger.ONE);
        } while (privateKey.p.compareTo(privateKey.q) == 0 || qMinusOne.gcd(publicKey.r).intValue()!=1);

        publicKey.n = privateKey.p.multiply(privateKey.q);
        privateKey.phi = pMinusOne.multiply(qMinusOne);
        itemZStarN(publicKey.n, privateKey.phi, publicKey.r);
        privateKey.x = publicKey.y.modPow(privateKey.phi.divide(publicKey.r), publicKey.n);
        System.out.println(privateKey.p + "\n " + privateKey.q + "\n" + publicKey.r + "\n" + publicKey.y);
    }

    private void itemZStarN(BigInteger n, BigInteger phi, BigInteger r) {
        do {
            publicKey.y = generatePrime(n.bitLength());
        } while (publicKey.y.compareTo(n) >= 0 || publicKey.y.gcd(n).intValue()
                != 1 || publicKey.y.modPow(phi.divide(r),n).intValue() == 1);
    }

    public BigInteger encrypt(int m){
        BigInteger u = randomZStarN(this.publicKey.n);
        BigInteger cipher1 = publicKey.y.modPow(BigInteger.valueOf(m),publicKey.n);
        BigInteger cipher2 = u.modPow(publicKey.r, publicKey.n);
        return cipher1.multiply(cipher2).mod(publicKey.n);
    }

    public int decrypt(BigInteger cipher){
        BigInteger a = cipher.modPow(this.privateKey.phi.divide(publicKey.r), publicKey.n);

        BigInteger c;
        for(int i = 0; i < publicKey.r.intValue(); i++){
            c = this.privateKey.x.modPow(BigInteger.valueOf(i), publicKey.n);
            if (a.equals(c)) return i;
        }
        return -1;
    }

    public static BigInteger randomZStarN(BigInteger n) {
        BigInteger u;
        do {
            u = new BigInteger(n.bitLength(), new
                    SecureRandom());
        } while (u.compareTo(n) >= 0 || u.gcd(n).intValue()
                != 1);
        return u;
    }


    public static void main(String[] args) {

        BenalohCipher b = new BenalohCipher(128);
        int[] intArray = {235345, 234345, 13445};
        ByteBuffer buffer = ByteBuffer.allocate(intArray.length * 4);
        int m_ = 147;
        BigInteger k = b.encrypt(m_);
        System.out.println(b.decrypt(k));
        for (int i = 0; i < intArray.length; i++){
            buffer.putInt(intArray[i]);
        }

        byte[] byteArray = buffer.array();
        byte[] byteArray_ = new byte[byteArray.length];
        for (int i = 0; i < byteArray.length; i++){
            int m = byteArray[i] & 0xff;
            BigInteger c = b.encrypt(m);
            System.out.println("decrypted : " + c);
            byte my = (byte) b.decrypt(c);
            byteArray_[i] = my;
            System.out.println("encrypted : " + b.decrypt(c));
        }
        ByteBuffer buf = ByteBuffer.wrap(byteArray_);
        int[] a = new int[byteArray_.length / 4];
        for (int i = 0; i < a.length; i++){
            a[i] = buf.getInt();
        }
        System.out.println(Arrays.toString(a));

        //int m = 20946;


        //BigInteger c = b.encrypt(m);
        //System.out.println("decrypted : " + c);
        //System.out.println("encrypted : " + b.decrypt(c));
    }

}

