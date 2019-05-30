package chat.dim.client;

import java.io.IOException;
import java.io.InputStream;

public class FileUtils {

    public static byte[] readResourceFile(String filename) throws IOException {
        InputStream is = FileUtils.class.getResourceAsStream(filename);
        assert is != null;
        int len = is.available();
        byte[] data = new byte[len];
        is.read(data, 0, len);
        return data;
    }

    public static String readTextFile(String filename) throws IOException {
        byte[] data = readResourceFile(filename);
        return new String(data, "UTF-8");
    }

    static {
        // mkm.Base64
        chat.dim.format.Base64.coder = new chat.dim.format.BaseCoder() {
            @Override
            public String encode(byte[] data) {
                return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            }

            @Override
            public byte[] decode(String string) {
                return android.util.Base64.decode(string, android.util.Base64.DEFAULT);
            }
        };
        // dkd.Base64
        chat.dim.dkd.Base64.coder = new chat.dim.dkd.BaseCoder() {
            @Override
            public String encode(byte[] data) {
                return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            }

            @Override
            public byte[] decode(String string) {
                return android.util.Base64.decode(string, android.util.Base64.DEFAULT);
            }
        };
    }
}
