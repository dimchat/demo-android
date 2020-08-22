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
import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Profile;

public class ProfileTable extends Database {

    // profile cache
    private Map<ID, Profile> profileTable = new HashMap<>();

    private boolean cache(Profile profile) {
        ID identifier = ID.getInstance(profile.getIdentifier());
        if (profile.isValid()) {
            profileTable.put(identifier, profile);
            return true;
        }
        return false;
    }

    // "/sdcard/chat.dim.sechat/mkm/{XX}/{address}/profile.js"
    private static String getProfilePath(ID entity) throws IOException {
        return getEntityFilePath(entity, "profile.js");
    }

    @SuppressWarnings("unchecked")
    private Profile loadProfile(ID entity) {
        try {
            String path = getProfilePath(entity);
            Object dict = loadJSON(path);
            return Profile.getInstance((Map<String, Object>) dict);
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
    }

    public boolean saveProfile(Profile profile) {
        if (!cache(profile)) {
            return false;
        }
        // write into JsON file
        ID identifier = ID.getInstance(profile.getIdentifier());
        try {
            String path = getProfilePath(identifier);
            return saveJSON(profile, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Profile getProfile(ID entity) {
        // 1. try from profile cache
        Profile profile = profileTable.get(entity);
        if (profile == null) {
            // 2. load from JsON file
            profile = loadProfile(entity);
            if (profile == null) {
                // 3. place an empty profile for cache
                profile = new Profile(entity);
            }
            // no need to verify profile from local storage
            profileTable.put(entity, profile);
        }
        return profile;
    }
}
