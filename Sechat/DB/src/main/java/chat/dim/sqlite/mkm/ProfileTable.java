/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.sqlite.mkm;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteCantOpenDatabaseException;

import java.util.HashMap;
import java.util.Map;

import chat.dim.Entity;
import chat.dim.format.Base64;
import chat.dim.mkm.BaseProfile;
import chat.dim.protocol.ID;
import chat.dim.protocol.Profile;
import chat.dim.sqlite.DataTable;
import chat.dim.utils.Log;

public final class ProfileTable extends DataTable implements chat.dim.database.ProfileTable {

    private ProfileTable() {
        super(EntityDatabase.getInstance());
    }

    private static ProfileTable ourInstance;
    public static ProfileTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new ProfileTable();
        }
        return ourInstance;
    }

    // memory caches
    private Map<String, Profile> profileTable = new HashMap<>();

    //
    //  chat.dim.database.UserTable
    //

    @Override
    public boolean saveProfile(Profile profile) {
        ID identifier = profile.getIdentifier();
        // 0. check duplicate record
        if (getProfile(identifier) != null) {
            Log.info("profile exists, update it: " + identifier);
            String[] whereArgs = {identifier.toString()};
            delete(EntityDatabase.T_PROFILE, "did=?", whereArgs);
        }

        String data = (String) profile.get("data");
        String base64 = (String) profile.get("signature");
        byte[] signature;
        if (base64 != null) {
            signature = Base64.decode(base64);
        } else {
            signature = null;
        }

        // 1. save into database
        ContentValues values = new ContentValues();
        values.put("did", identifier.toString());
        values.put("data", data);
        values.put("signature", signature);
        if (insert(EntityDatabase.T_PROFILE, null, values) < 0) {
            return false;
        }
        Log.info("-------- profile updated: " + identifier);

        // 2. store into memory cache
        profileTable.put(identifier.toString(), profile);
        return true;
    }

    @Override
    public Profile getProfile(ID entity) {
        // 1. try from memory cache
        Profile profile = profileTable.get(entity.toString());
        if (profile != null) {
            return profile;
        }

        // 2. try from database
        String[] columns = {"data", "signature"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(EntityDatabase.T_PROFILE, columns, "did=?", selectionArgs, null, null, null)) {
            String data;
            byte[] signature;
            Map<String, Object> info;
            if (cursor.moveToNext()) {
                data = cursor.getString(0);
                signature = cursor.getBlob(1);

                info = new HashMap<>();
                info.put("ID", entity.toString());
                info.put("data", data);
                info.put("signature", Base64.encode(signature));

                profile = Entity.parseProfile(info);
            }
        } catch (SQLiteCantOpenDatabaseException e) {
            e.printStackTrace();
        }
        if (profile == null) {
            // 2.1. place an empty profile for cache
            profile = new BaseProfile(entity);
        }

        // 3. store into memory cache
        profileTable.put(entity.toString(), profile);
        return profile;
    }
}
