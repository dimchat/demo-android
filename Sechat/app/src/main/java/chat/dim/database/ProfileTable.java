package chat.dim.database;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Profile;

public class ProfileTable extends Database {

    // load profile from "/sdcard/chat.dim.sechat/.mkm/{address}/profile.js"
    public static Profile loadProfile(ID identifier) {
        try {
            // load from JsON file
            Map<String, Object> dict = loadJSONFile("profile.js", identifier);
            return Profile.getInstance(dict);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    // save profile into "/sdcard/chat.dim.sechat/.mkm/{address}/profile.js"
    public static boolean saveProfile(Profile profile) {
        try {
            // save into JsON file
            return saveJSONFile(profile, "profile.js", profile.identifier, true);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
