package des;

public
 class FeistelFunction implements Cryption {
    DesKeyExpansion expansion;
    DesEncrypt encrypt;

    public FeistelFunction(KeyExpansion expansion, Encrypt encrypt) {

    }

    @Override
    public byte[] encrypt(byte[] array) {
        return new byte[0];
    }

    @Override
    public byte[] decrypt(byte[] array) {
        return new byte[0];
    }

    @Override
    public byte[][] setRoundKeys(byte[] key) {
        return new byte[0][];
    }
}
