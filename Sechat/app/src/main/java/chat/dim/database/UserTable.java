package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;

public class UserTable extends ExternalStorage {

    private static List<ID> contactList = new ArrayList<>();

    public static void reloadData(ID user) {
        // TODO: reload contacts for current user
        contactList.add(ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj"));
        contactList.add(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));
    }

    public static List<ID> getContacts(ID user) {
        return contactList;
    }

    //-------- Private Key

    private static Map<Address, PrivateKey> keys = new HashMap<>();

    // "/sdcard/chat.dim.sechat/.private/{address}/secret.js"

    private static String getKeyFilePath(Address address) {
        return root + "/.private/" + address + "/secret.js";
    }

    private static PrivateKey loadKey(Address address) {
        // load from JsON file
        String path = getKeyFilePath(address);
        try {
            Object dict = readJSON(path);
            return PrivateKeyImpl.getInstance(dict);
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            return null;
        }
    }

    public static PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = keys.get(user.address);
        if (key == null) {
            key = loadKey(user.address);
            if (key != null) {
                keys.put(user.address, key);
            }
        }
        return key;
    }

    public static List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        // FIXME: get private key matches profile key
        PrivateKey key = getPrivateKeyForSignature(user);
        if (key == null) {
            return null;
        }
        List<PrivateKey> keys = new ArrayList<>();
        keys.add(key);
        return keys;
    }

    public static boolean savePrivateKey(PrivateKey key, ID identifier) {
        keys.put(identifier.address, key);
        String path = getKeyFilePath(identifier.address);
        try {
            return writeJSON(key, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
