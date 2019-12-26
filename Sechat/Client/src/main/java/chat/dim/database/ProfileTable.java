/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.database;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.filesys.ExternalStorage;

public class ProfileTable extends ExternalStorage {

    // profile cache
    private Map<ID, Profile> profileTable = new HashMap<>();

    // "/sdcard/chat.dim.sechat/mkm/{address}/profile.js"

    private static String getProfilePath(ID entity) {
        return getPath() + "/mkm/" + entity.address + "/profile.js";
    }

    private boolean cacheProfile(Profile profile) {
        ID identifier = ID.getInstance(profile.getIdentifier());
        if (profile.isValid()) {
            profileTable.put(identifier, profile);
            return true;
        }
        return false;
    }

    private Profile loadProfile(ID entity) {
        String path = getProfilePath(entity);
        try {
            Object dict = loadJSON(path);
            return Profile.getInstance(dict);
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
    }

    public boolean saveProfile(Profile profile) {
        if (!cacheProfile(profile)) {
            return false;
        }
        // write into JsON file
        ID identifier = ID.getInstance(profile.getIdentifier());
        String path = getProfilePath(identifier);
        try {
            return saveJSON(profile, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Profile getProfile(ID entity) {
        // 1. try from profile cache
        Profile profile = profileTable.get(entity);
        if (profile != null) {
            // check cache expires
            Date now = new Date();
            Object timestamp = profile.get("lastTime");
            if (timestamp == null) {
                profile.put("lastTime", now.getTime() / 1000);
            } else {
                Date lastTime = new Date(((Number) timestamp).longValue() * 1000);
                long dt = now.getTime() - lastTime.getTime();
                if (Math.abs(dt / 1000) > 3600) {
                    // profile expired
                    profileTable.remove(entity);
                }
            }
            return profile;
        }
        // TODO: 2. send query profile for updating from network

        // 3. load from JsON file
        profile = loadProfile(entity);
        if (profile == null) {
            profile = new Profile(entity);
        }
        profileTable.put(entity, profile);
        return profile;
    }
}
