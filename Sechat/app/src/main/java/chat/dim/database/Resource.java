package chat.dim.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import chat.dim.format.JSON;

public class Resource {

    // "/sdcard/chat.dim.sechat/private"
    public static String privateDirectory = "/tmp/.dimp-android/";

    // "/sdcard/chat.dim.sechat/public"
    public static String publicDirectory = "/tmp/sechat-android/";

    /**
     *  Save content into a file in the directory
     *  if the file exists, overwrite it.
     *
     * @param data - binary content
     * @param filename - file name
     * @param directory - special directory
     * @return true on success
     * @throws IOException on error
     */
    static boolean writeFile(byte[] data, String filename, String directory) throws IOException {
        assert directory != null;
        assert filename != null;
        assert data != null;
        File file = new File(directory, filename);
        if (!file.exists()) {
            // check parent directory exists
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
        return true;
    }

    static boolean writeTextFile(String text, String filename, String directory) throws IOException {
        return writeFile(text.getBytes(Charset.forName("UTF-8")), filename, directory);
    }

    static boolean writeJSONFile(Map dictionary, String filename, String directory) throws IOException {
        return writeTextFile(JSON.encode(dictionary), filename, directory);
    }
    static boolean writeJSONFile(List array, String filename, String directory) throws IOException {
        return writeTextFile(JSON.encode(array), filename, directory);
    }

    /**
     *  Save content into a file in the directory,
     *  if the file exists, ignore it.
     *
     * @param data - binary content
     * @param filename - file name
     * @param directory - special directory
     * @return true on success
     * @throws IOException on error
     */
    static boolean saveFile(byte[] data, String filename, String directory) throws IOException {
        assert directory != null;
        assert filename != null;
        assert data != null;
        File file = new File(directory, filename);
        if (!file.exists()) {
            // check parent directory exists
            File dir = file.getParentFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
        } else {
            // no need to update meta file
            return true;
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
        return true;
    }

    static boolean saveTextFile(String text, String filename, String directory) throws IOException {
        return saveFile(text.getBytes(Charset.forName("UTF-8")), filename, directory);
    }

    static boolean saveJSONFile(Map dictionary, String filename, String directory) throws IOException {
        return saveTextFile(JSON.encode(dictionary), filename, directory);
    }

    //-------- read file

    /**
     *  Read content from a file in the directory
     *
     * @param filename - file name
     * @param directory - special directory
     * @return binary content
     * @throws IOException on error
     */
    static byte[] readFile(String filename, String directory) throws IOException {
        assert directory != null;
        assert filename != null;
        File file = new File(directory, filename);
        if (!file.exists()) {
            // file not found
            return null;
        }
        // load from JsON file
        InputStream fis = new FileInputStream(file);
        int size = fis.available();
        byte[] data = new byte[size];
        int len = fis.read(data);
        fis.close();
        if (len != size) {
            throw new IOException("reading error: " + len + " != " + size
                    + ", file: " + filename + ", dirctory: " + directory);
        }
        return data;
    }

    static String readTextFile(String filename, String directory) throws IOException {
        byte[] data = readFile(filename, directory);
        return data == null ? null : new String(data, Charset.forName("UTF-8"));
    }

    static Map<String, Object> readJSONFile(String filename, String directory) throws IOException {
        String json = readTextFile(filename, directory);
        return json == null ? null : JSON.decode(json);
    }

    /**
     *  Read content from a file in application's resources directory
     *
     * @param filename - resource file name
     * @return binary content
     * @throws IOException on error
     */
    static byte[] readFile(String filename) throws IOException {
        assert filename != null;
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

    static String readTextFile(String filename) throws IOException {
        byte[] data = readFile(filename);
        return data == null ? null : new String(data, Charset.forName("UTF-8"));
    }

    static Map<String, Object> readJSONFile(String filename) throws IOException {
        String json = readTextFile(filename);
        return json == null ? null : JSON.decode(json);
    }

    /**
     *  Remove file in the directory
     *
     * @param filename - file name
     * @param directory - special directory
     * @return true on success
     */
    static boolean removeFile(String filename, String directory) {
        assert directory != null;
        assert filename != null;
        File file = new File(directory, filename);
        if (!file.exists()) {
            // file not found
            return true;
        }
        return !file.isDirectory() && file.delete();
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
