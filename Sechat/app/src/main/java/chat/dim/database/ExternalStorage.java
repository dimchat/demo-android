package chat.dim.database;

import android.os.Environment;

import java.io.IOException;
import java.nio.charset.Charset;

import chat.dim.filesys.Storage;
import chat.dim.format.JSON;
import chat.dim.utils.Log;

public class ExternalStorage {

    public static final String root;

    static {
        String path = Environment.getExternalStorageDirectory().getPath();
        root = path + "/chat.dim.sechat";
    }

    //-------- read

    public static boolean exists(String filename) throws IOException {
        Storage file = new Storage();
        return file.exists(filename);
    }

    public static byte[] read(String filename) throws IOException {
        Storage file = new Storage();
        file.load(filename);
        return file.getData();
    }

    public static String readText(String filename) throws IOException {
        byte[] data = read(filename);
        if (data == null) {
            return null;
        }
        return new String(data, Charset.forName("UTF-8"));
    }

    public static Object readJSON(String filename) throws IOException {
        String string = readText(filename);
        if (string == null) {
            return null;
        }
        return JSON.decode(string);
    }

    //-------- write

    public static boolean write(byte[] data, String filename) throws IOException {
        Storage file = new Storage();
        file.setData(data);
        int len = file.save(filename);
        Log.info("wrote " + len + " byte(s) into " + filename);
        return len == data.length;
    }

    public static boolean writeText(String text, String filename) throws IOException {
        byte[] data = text.getBytes(Charset.forName("UTF-8"));
        return write(data, filename);
    }

    public static boolean writeJSON(Object object, String filename) throws IOException {
        String json = JSON.encode(object);
        return writeText(json, filename);
    }

    public static boolean delete(String filename) throws IOException {
        Storage file = new Storage();
        return file.remove(filename);
    }
}
