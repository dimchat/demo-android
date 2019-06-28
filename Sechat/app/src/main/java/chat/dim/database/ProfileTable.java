package chat.dim.database;

import java.io.IOException;
import java.util.Map;

import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Profile;

public class ProfileTable extends Database {

    // "/sdcard/chat.dim.sechat/mkm/{address}/profile.js"

    static String getProfileDirectory(Address address) {
        return publicDirectory + "/mkm/" + address;
    }
    static String getProfileDirectory(ID identifier) {
        return getProfileDirectory(identifier.address);
    }

    public static Profile loadProfile(ID identifier) {
        // load from JsON file
        String dir = getProfileDirectory(identifier.address);
        try {
            Map<String, Object> dict = readJSONFile("profile.js", dir);
            return Profile.getInstance(dict);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean saveProfile(Profile profile) {
        // write into JsON file
        String dir = getProfileDirectory(profile.identifier);
        try {
            return writeJSONFile(profile, "profile.js", dir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
