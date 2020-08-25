import java.util.Arrays;
import java.util.Random;

/*!
 * compile group: 'chat.dim', name: 'Plugins', version: '0.1.0'
 */
import chat.dim.digest.SHA256;
import chat.dim.format.Hex;
import chat.dim.format.Plugins;
import chat.dim.format.UTF8;

public class SeedBox {

    public static final int COUNT = 6;  // count of result numbers

    private static final int SIZE = 32; // secret seed size
    private final byte[] seed;          // secret seed (random number)

    public SeedBox() {
        super();
        seed = new byte[SIZE];
        // init seed
        Random random = new Random();
        random.nextBytes(seed);
    }

    public SeedBox(byte[] secret) {
        super();
        // copy secret seed
        assert secret.length == SIZE : "secret error: " + Arrays.toString(secret);
        seed = secret;
    }

    /**
     *  Formula:
     *      Pi = h(Si)
     *
     *      h(d) = sha256(sha256(d))
     *
     * @return digest of secret random number
     */
    public byte[] getProof() {
        return SHA256.digest(SHA256.digest(seed));
    }

    public String getProofHex() {
        return Hex.encode(getProof());
    }

    /**
     *  Formula:
     *      X = g(sigma(Si))
     *
     *      g(d) = sha256(sha256(d))
     *
     * @param boxes - boxes with secret seeds
     * @return generated random number from secret seeds
     */
    private static byte[] generate(SeedBox[] boxes) {
        int count = boxes.length;
        int length = SIZE * count;
        byte[] buffer = new byte[length];
        // S = S0 + S1 + S2 + ...
        for (int i = 0; i < count; ++i) {
            SeedBox box = boxes[i];
            System.arraycopy(box.seed, 0, buffer, SIZE * i, SIZE);
        }
        // X = g(S)
        return SHA256.digest(SHA256.digest(buffer));
    }

    /**
     *  Formula:
     *      Ni = f(X, i)
     *
     *      f(X, i) = int(X/(256^i)) mod 32
     *              = (X >> (8*i)) & 0x1F
     *
     * @param X - generated random number
     * @param i - position
     * @return result number
     */
    private static int number(byte[] X, int i) {
        assert i < X.length : "generated random number error";
        int pos = X.length - 1 - i;
        int num = X[pos] & 0x1F; // 0001 1111
        if (num == 0) {
            num = 0x20; // 0010 0000
        }
        return num;
    }

    public static int[] getResults(SeedBox[] boxes) {
        // 1. generate the "random" number with all seeds
        byte[] X = generate(boxes);
        // 2. get results
        int[] res = new int[COUNT];
        for (int i = 0; i < COUNT; ++i) {
            res[i] = number(X, i);
        }
        return res;
    }

    /**
     *  Test case
     *
     * @param args - command arguments
     */
    public static void main(String[] args) {

        final int sp_count = 8;
        SeedBox[] boxes = new SeedBox[sp_count];

        // 1. generate random seed for each boxes
        String s = String.format("\n==== generate with %d service providers ====", sp_count);
        System.out.println(s);
        for (int i = 0; i < sp_count; ++i) {
            SeedBox box = new SeedBox();
            boxes[i] = box;
            System.out.println(String.format("box[%d]: %s", i, box.getProofHex()));
        }

        // 2. get random number from this boxes
        int[] res = getResults(boxes);

        // show results
        System.out.println("\n---- result numbers ----");
        for (int num : res) {
            System.out.print(String.format(" %02d ", num));
        }
        System.out.println("\n---- result numbers ----");
    }

    static {
        final Plugins plugins = new Plugins() {}; // active HEX encoder implementation

        // SHA256 + HEX test
        String string = "moky";
        byte[] data = UTF8.encode(string);
        byte[] digest = SHA256.digest(data);
        String hex = Hex.encode(digest);
        if (!hex.equals("cb98b739dd699aa44bb6ebba128d20f2d1e10bb3b4aa5ff4e79295b47e9ed76d")) {
            throw new ArithmeticException("SHA256 error");
        }
        System.out.println(String.format("SHA256(%s) = %s", string, hex));
    }
}
