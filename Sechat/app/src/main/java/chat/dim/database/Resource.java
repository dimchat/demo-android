package chat.dim.database;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class Resource {

    private static byte[] readResourceFile(String filename) throws IOException {
        InputStream is = Resource.class.getResourceAsStream(filename);
        assert is != null;
        int size = is.available();
        byte[] data = new byte[size];
        int len = is.read(data, 0, size);
        if (len != size) {
            throw new IOException("read error: " + len + " != " + size);
        }
        return data;
    }

    public static String readTextFile(String filename) throws IOException {
        byte[] data = readResourceFile(filename);
        return new String(data, Charset.forName("UTF-8"));
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
