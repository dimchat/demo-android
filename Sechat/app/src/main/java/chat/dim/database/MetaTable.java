package chat.dim.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import chat.dim.format.JSON;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class MetaTable extends Database {

    // load meta from "/sdcard/chat.dim.sechat/.mkm/{address}/meta.js"
    public static Meta loadMeta(ID identifier) {
        try {
            // load from JsON file
            Map<String, Object> dict = loadJSONFile("meta.js", identifier);
            return Meta.getInstance(dict);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    // save meta into "/sdcard/chat.dim.sechat/.mkm/{address}/meta.js"
    public static boolean saveMeta(Meta meta, ID identifier) {
        // check whether match ID
        if (!meta.matches(identifier)) {
            throw new ArithmeticException("meta not match ID: " + identifier + ", " + meta);
        }
        try {
            // save into JsON file
            return saveJSONFile(meta, "meta.js", identifier, false);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
