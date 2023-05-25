package server.cryptoserver.algorithms;

import java.io.BufferedWriter;
import java.math.BigInteger;
import java.util.Random;

public class SolovayStrassenTest implements PrimeChecker{

    public boolean isPrime(BigInteger number, int iterations) {
        // Обработка базовых случаев
        if (number.equals(BigInteger.ONE) || number.equals(BigInteger.TWO)) {
            return true;
        }
        if (number.compareTo(BigInteger.TWO) < 0 || number.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            return false;
        }

        Random random = new Random();

        for (int i = 0; i < iterations; i++) {
            //выбираем случайное а от 2 до n - 1
            BigInteger a = new BigInteger(number.bitLength(), random).mod(number.subtract(BigInteger.TWO)).add(BigInteger.TWO);

            if (!witness(a, number)) {
                return false;
            }
        }

        return true;
    }

    public static boolean witness(BigInteger a, BigInteger number) {
        BigInteger exponent = number.subtract(BigInteger.ONE).divide(BigInteger.TWO); // (n - 1)/2
        BigInteger result = a.modPow(exponent, number); // a ^ (n-1)/2 mod n
        if (!a.gcd(number).equals(BigInteger.ONE))
            return false;

        BigInteger myJacobiSymbol = jacobiSymbol(a, number);

        return result.equals(myJacobiSymbol);
    }

    public static BigInteger jacobiSymbol(BigInteger a, BigInteger b) {
        // вычисление символа якоби. он равен произведению символов Лежандра
        //символ лежандра (a/p) = 0 если a делится на p ,
        //(a/p) = 1 если a является квадратичным вычетом по модулю p т.е есть число которое x^2 сравн с а по мод p
        // -1 если невычет

        if (b.compareTo(BigInteger.ONE) <= 0 || b.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
            throw new IllegalArgumentException("b должно быть положительным нечетным числом");
        }

        //nod
        if (!a.gcd(b).equals(BigInteger.ONE)){
            return BigInteger.ZERO;
        }

        BigInteger r = BigInteger.ONE;
        BigInteger temp;


        //переход к положительным числам
        if (a.compareTo(BigInteger.ZERO) < 0) {
            a = a.negate();
            if (b.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))) {
                r = r.negate();
            }
        }

        //избавление от четности
        BigInteger t = BigInteger.valueOf(0);
        while (!a.equals(BigInteger.ZERO)) {
            while (a.mod(BigInteger.TWO).equals(BigInteger.ZERO)) {
                a = a.divide(BigInteger.TWO);
                t = t.add(BigInteger.ONE);
            }

            if (t.mod(BigInteger.TWO).equals(BigInteger.ONE)) {
                if (b.mod(BigInteger.valueOf(8)).equals(BigInteger.valueOf(3))
                        || b.mod(BigInteger.valueOf(8)).equals(BigInteger.valueOf(5))) {
                    r = r.negate();
                }
            }

            //квадратичный закон взаимности

            if (a.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))
                    && b.mod(BigInteger.valueOf(4)).equals(BigInteger.valueOf(3))) {
                r = r.negate();
            }

            temp = a;
            a = b.mod(temp);
            b = temp;

            //a = a.mod(b);
        }
        return r;

    }
}
