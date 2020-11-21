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

import java.util.HashMap;
import java.util.Map;

import chat.dim.Entity;
import chat.dim.crypto.VerifyKey;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;
import chat.dim.sqlite.DataTable;
import chat.dim.utils.Log;

public final class MetaTable extends DataTable implements chat.dim.database.MetaTable {

    private MetaTable() {
        super(EntityDatabase.getInstance());
    }

    private static MetaTable ourInstance;
    public static MetaTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new MetaTable();
        }
        return ourInstance;
    }

    // memory caches
    private Map<ID, Meta> metaTable = new HashMap<>();

    //
    //  chat.dim.database.UserTable
    //

    @Override
    public boolean saveMeta(Meta meta, ID entity) {
        // 0. check duplicate record
        if (getMeta(entity) != null) {
            // meta won't change, no need to update
            Log.info("meta exists: " + entity);
            return true;
        }
        VerifyKey key = meta.getKey();
        byte[] data = JSON.encode(key);
        String pk = UTF8.decode(data);

        // 1. save into database
        ContentValues values = new ContentValues();
        values.put("did", entity.toString());
        values.put("version", meta.getType());
        values.put("pk", pk);
        values.put("seed", meta.getSeed());
        values.put("fingerprint", meta.getFingerprint());
        if (insert(EntityDatabase.T_META, null, values) < 0) {
            return false;
        }
        Log.info("-------- meta saved: " + entity);

        // 2. store into memory cache
        metaTable.put(entity, meta);
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Meta getMeta(ID entity) {
        // 1. try from memory cache
        Meta meta = metaTable.get(entity);
        if (meta != null) {
            return meta;
        }

        // 2. try from database
        String[] columns = {"version", "pk", "seed", "fingerprint"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(EntityDatabase.T_META, columns, "did=?", selectionArgs, null, null, null)) {
            int version;
            String pk;
            byte[] data;
            Map<String, Object> key;
            String seed;
            byte[] fp;
            Map<String, Object> info;
            if (cursor.moveToNext()) {
                version = cursor.getInt(0);
                pk = cursor.getString(1);
                data = UTF8.encode(pk);
                key = (Map<String, Object>) JSON.decode(data);

                info = new HashMap<>();
                info.put("version", version);
                info.put("key", key);
                if (MetaType.hasSeed(version)) {
                    seed = cursor.getString(2);
                    fp = cursor.getBlob(3);
                    info.put("seed", seed);
                    info.put("fingerprint", Base64.encode(fp));
                }
                meta = Entity.parseMeta(info);
            }
        }
        if (meta == null) {
            return null;
        }

        // 3. store into memory cache
        metaTable.put(entity, meta);
        return meta;
    }
}
