import java.nio.charset.Charset;
import java.util.Random;

/*!
 * compile group: 'chat.dim', name: 'Plugins', version: '0.1.0'
 */
import chat.dim.digest.SHA256;
import chat.dim.format.Hex;
import chat.dim.format.Plugins;

public class SeedBox {

    public static final int COUNT = 6;  // count of result numbers

    private static final int SIZE = 32; // secret seed size
    private final byte[] seed;          // secret seed (random number)

    public SeedBox() {
        super();
        // init seed
        Random random = new Random();
        seed = new byte[SIZE];
        random.nextBytes(seed);
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
        byte[] digest = SHA256.digest(seed);
        digest = SHA256.digest(digest);
        return digest;
    }

    public String getProofHex() {
        byte[] proof = getProof();
        return Hex.encode(proof);
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
    public static byte[] generate(SeedBox[] boxes) {
        int count = boxes.length;
        int length = SIZE * count;
        byte[] buffer = new byte[length];
        // S = S0 + S1 + S2 + ...
        for (int i = 0; i < count; ++i) {
            SeedBox box = boxes[i];
            System.arraycopy(box.seed, 0, buffer, SIZE * i, SIZE);
        }
        // X = hash(S)
        byte[] digest = SHA256.digest(buffer);
        digest = SHA256.digest(digest);
        return digest;
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
    public static int getResult(byte[] X, int i) {
        assert i < X.length : "generated random number error";
        int pos = X.length - 1 - i;
        return X[pos] & 0x1F; // 0001 1111
    }

    /**
     *  Test case
     *
     * @param args - command arguments
     */
    public static void main(String args[]) {

        final int sp_count = 8;
        SeedBox[] boxes = new SeedBox[sp_count];

        // 1. init
        System.out.println(String.format(">>>> generate with %d service providers", sp_count));
        for (int i = 0; i < sp_count; ++i) {
            SeedBox box = new SeedBox();
            boxes[i] = box;
            System.out.println(String.format("box[%d]: %s", i, box.getProofHex()));
        }

        // 2. generate random number
        byte[] X = generate(boxes);

        // 3. get result
        System.out.println(String.format("---- %d result numbers ----", COUNT));
        for (int i = 0; i < COUNT; ++i) {
            int res = getResult(X, i);
            if (res == 0) {
                res = 32;
            }
            System.out.println(String.format("result[%d]: %02d", i, res));
        }
    }

    static {
        final Plugins plugins = new Plugins() {}; // active HEX encoder implementation

        // SHA256 + HEX test
        String string = "xiao";
        byte[] data = string.getBytes(Charset.forName("UTF-8"));
        byte[] digest = SHA256.digest(data);
        String hex = Hex.encode(digest);
        assert hex.equals("6941ce7bf0dc5b77b3c8876e8018830b67c60474f9ee3de608e27b390873fe31") : "SHA256 error";
        System.out.println(String.format("SHA256(%s) = %s", string, hex));
    }
}
