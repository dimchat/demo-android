package chat.dim.database;

import java.io.IOException;

import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class MetaTable extends ExternalStorage {

    // "/sdcard/chat.dim.sechat/mkm/{address}/meta.js"

    private static String getFilePath(Address address) {
        return root + "/mkm/" + address + "/meta.js";
    }

    private static Meta loadMeta(Address address) {
        // load from JsON file
        String path = getFilePath(address);
        try {
            Object dict = readJSON(path);
            return Meta.getInstance(dict);
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            return null;
        }
    }

    public static ID getID(Address address) {
        Meta meta = loadMeta(address);
        if (meta == null) {
            return null;
        }
        return meta.generateID(address.getNetwork());
    }

    public static boolean saveMeta(Meta meta, ID identifier) {
        // save into JsON file
        String path = getFilePath(identifier.address);
        try {
            return writeJSON(meta, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Meta getMeta(ID identifier) {
        return loadMeta(identifier.address);
    }
}
