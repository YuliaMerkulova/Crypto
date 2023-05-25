package server.cryptoserver.algorithms;

import java.math.BigInteger;

public interface PrimeChecker {

    public boolean isPrime(BigInteger num, int k);
}
