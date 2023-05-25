package client.cryptoclient.algorithms;

import java.math.BigInteger;
import java.util.Random;

public class BenalohHelper {
    // Функция проверки числа на простоту с помощью теста Миллера-Рабина



    public static BigInteger generatePrime(int bitLength, PrimeChecker checker) {
        BigInteger prime;
        Random random = new Random();
        do {
            prime = new BigInteger(bitLength, random);
        } while (!checker.isPrime(prime,100));
        return prime;
    }

}
