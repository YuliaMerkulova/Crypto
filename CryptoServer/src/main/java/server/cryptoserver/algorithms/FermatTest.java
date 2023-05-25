package server.cryptoserver.algorithms;

import java.math.BigInteger;
import java.util.Random;

public class FermatTest implements PrimeChecker{

    public boolean isPrime(BigInteger number, int iterations) {
        if (number.equals(BigInteger.ONE)) {
            return false;
        }

        if (number.equals(BigInteger.TWO)) {
            return true;
        }

        Random random = new Random();

        for (int i = 0; i < iterations; i++) {
            BigInteger a = new BigInteger(number.bitLength(), random);
            //выбираем число до n - 1
            a = a.mod(number.subtract(BigInteger.TWO)).add(BigInteger.TWO);

            if (!witness(a, number)) {
                return false;
            }
        }

        return true;
    }

    private static boolean witness(BigInteger a, BigInteger number) {
        BigInteger result = a.modPow(number.subtract(BigInteger.ONE), number);

        return result.equals(BigInteger.ONE);
    }
}
