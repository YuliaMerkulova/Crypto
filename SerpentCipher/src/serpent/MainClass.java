package serpent;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;

public class MainClass {
    private static Random randomizer = new Random(LocalDateTime.now().getNano());
    public static void main(String[] args) {
        SerpentCipher serpentCipher = new SerpentCipher(generateKey(4));
        byte[] array = generateByteArray(16);
        System.out.println((Arrays.toString(array)));
        System.out.println((Arrays.toString(serpentCipher.decrypt(serpentCipher.encrypt(array)))));
    }
    public static int[] generateKey(int len) {  // len = 4/6/8
        int[] key = new int[len];
        for (int i = 0; i < len; i++) {
            key[i] = randomizer.nextInt();
        }
        return key;
    }

    public static byte[] generateByteArray(int len) {  // len = 4/6/8
        byte[] array = new byte[len];
        randomizer.nextBytes(array);
        return array;
    }
}
