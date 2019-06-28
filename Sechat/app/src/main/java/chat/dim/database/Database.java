package chat.dim.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import chat.dim.format.JSON;
import chat.dim.mkm.entity.ID;

class Database {

    // "/sdcard/chat.dim.sechat/.mkm/"
    private static String publicDirectory = "/tmp/sechat-android/.mkm/";

    private static String getPublicDirectory(ID identifier) {
        // TODO: make sure sub dirs exist
        return publicDirectory + identifier.address;
    }

    // "/sdcard/chat.dim.sechat/.mkm/{address}/{filename}"
    static Map<String, Object> loadJSONFile(String filename, ID identifier)
            throws IOException {
        assert publicDirectory != null;
        File file = new File(getPublicDirectory(identifier), filename);
        if (!file.exists()) {
            // meta file not found
            return null;
        }
        // load from JsON file
        FileInputStream fis = new FileInputStream(file);
        int size = fis.available();
        byte[] data = new byte[size];
        int len = fis.read(data);
        fis.close();
        if (len != size) {
            throw new IOException("reading error: " + len + " != " + size + ", file: " + filename);
        }
        String json = new String(data, Charset.forName("UTF-8"));
        return JSON.decode(json);
    }

    // "/sdcard/chat.dim.sechat/.mkm/{address}/{filename}"
    static boolean saveJSONFile(Map dictionary, String filename, ID identifier, boolean overwrite)
            throws IOException {
        // check default directory
        File file = new File(getPublicDirectory(identifier), filename);
        if (!overwrite && file.exists()) {
            // no need to update meta file
            return true;
        }
        // save into JsON file
        String json = JSON.encode(dictionary);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
        return true;
    }
}
