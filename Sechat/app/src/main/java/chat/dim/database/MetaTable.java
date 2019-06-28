package chat.dim.database;

import java.io.IOException;
import java.util.Map;

import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class MetaTable extends Database {

    // "/sdcard/chat.dim.sechat/mkm/{address}/meta.js"

    static String getMetaDirectory(Address address) {
        return publicDirectory + "/mkm/" + address;
    }
    static String getMetaDirectory(ID identifier) {
        return getMetaDirectory(identifier.address);
    }

    /**
     *  Load meta for entity ID
     *
     * @param identifier - entity ID
     * @return meta info
     */
    public static Meta loadMeta(ID identifier) {
        return loadMeta(identifier.address);
    }

    public static Meta loadMeta(Address address) {
        // load from JsON file
        String dir = getMetaDirectory(address);
        try {
            Map<String, Object> dict = readJSONFile("meta.js", dir);
            return Meta.getInstance(dict);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Save meta for entity ID
     *
     * @param meta - meta info
     * @param identifier - entity ID
     * @return false on error
     */
    public static boolean saveMeta(Meta meta, ID identifier) {
        return saveMeta(meta, identifier.address);
    }

    public static boolean saveMeta(Meta meta, Address address) {
        // save into JsON file
        String dir = getMetaDirectory(address);
        try {
            return saveJSONFile(meta, "meta.js", dir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *  Get ID with address
     *
     * @param address - ID.address
     * @return ID
     */
    public static ID getID(Address address) {
        Meta meta = loadMeta(address);
        //return new ID(meta.seed, address);
        return meta == null ? null : meta.generateID(address.getNetwork());
    }
}
