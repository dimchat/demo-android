package chat.dim.database;

import java.io.IOException;
import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.mkm.entity.Profile;

public class MetaTable extends Resource {

    // "/sdcard/chat.dim.sechat/mkm/{address}/meta.js"

    private static String getMetaDirectory(Address address) {
        return publicDirectory + "/mkm/" + address;
    }
    private static String getMetaDirectory(ID identifier) {
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

    public static ID getID(Address address) {
        Meta meta = loadMeta(address);
        //return new ID(meta.seed, address);
        return meta == null ? null : meta.generateID(address.getNetwork());
    }

    public static boolean saveMeta(Meta meta, ID identifier) {
        // save into JsON file
        String dir = getMetaDirectory(identifier);
        try {
            return saveJSONFile(meta, "meta.js", dir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static Meta getMeta(ID identifier) {
        return loadMeta(identifier.address);
    }

    public static Profile getProfile(ID identifier) {
        // TODO: load profile from local storage
        return null;
    }

    public static boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        // TODO: save private key for ID
        return false;
    }

    public static boolean saveProfile(Profile profile) {
        // TODO: save profile to local storage
        return false;
    }
}
