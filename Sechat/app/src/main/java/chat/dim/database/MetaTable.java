package chat.dim.database;

import java.io.IOException;

import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class MetaTable extends ExternalStorage {

    // "/sdcard/chat.dim.sechat/mkm/{address}/meta.js"

    private static String getMetaFilePath(ID entity) {
        return root + "/mkm/" + entity.address + "/meta.js";
    }

    private static Meta loadMeta(ID entity) {
        // load from JsON file
        String path = getMetaFilePath(entity);
        try {
            Object dict = readJSON(path);
            return Meta.getInstance(dict);
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            return null;
        }
    }

//    public static ID getID(Address address) {
//        Meta meta = loadMeta(address);
//        if (meta == null) {
//            return null;
//        }
//        return meta.generateID(address.getNetwork());
//    }

    public static boolean saveMeta(Meta meta, ID entity) {
        // save into JsON file
        String path = getMetaFilePath(entity);
        try {
            return writeJSON(meta, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Meta getMeta(ID entity) {
        return loadMeta(entity);
    }
}
