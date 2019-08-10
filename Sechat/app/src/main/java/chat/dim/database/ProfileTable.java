package chat.dim.database;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Profile;

public class ProfileTable extends ExternalStorage {

    // profile cache
    static Map<Address, Profile> profileTable = new HashMap<>();

    // "/sdcard/chat.dim.sechat/mkm/{address}/profile.js"

    static String getFilePath(Address address) {
        return root + "/mkm/" + address + "/profile.js";
    }

    private static Profile loadProfile(Address address) {
        String path = getFilePath(address);
        try {
            Object dict = readJSON(path);
            return Profile.getInstance(dict);
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
    }

    public static Profile getProfile(ID identifier) {
        // 1. try from profile cache
        Profile profile = profileTable.get(identifier.address);
        if (profile != null) {
            // check cache expires
            Date now = new Date();
            Object timestamp = profile.get("lastTime");
            if (timestamp == null) {
                profile.put("lastTime", now.getTime() / 1000);
            } else {
                Date lastTime = new Date((long) timestamp * 1000);
                long dt = now.getTime() - lastTime.getTime();
                if (Math.abs(dt / 1000) > 3600) {
                    // profile expired
                    profileTable.remove(identifier.address);
                }
            }
            return profile;
        }
        // TODO: 2. send query profile for updating from network

        // 3. load from JsON file
        profile = loadProfile(identifier.address);
        if (profile != null) {
            profileTable.put(identifier.address, profile);
        }
        return profile;
    }

    public static boolean saveProfile(Profile profile) {
        // write into JsON file
        String path = getFilePath(profile.identifier.address);
        try {
            return writeJSON(profile, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
