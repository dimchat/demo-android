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

import chat.dim.crypto.PublicKey;
import chat.dim.format.EncodeAlgorithms;
import chat.dim.format.JSON;
import chat.dim.format.TransportableData;
import chat.dim.mkm.BaseMeta;
import chat.dim.mkm.MetaUtils;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaVersion;
import chat.dim.sqlite.DataTable;
import chat.dim.sqlite.Database;
import chat.dim.utils.Log;

public final class MetaTable extends DataTable implements chat.dim.database.MetaTable {

    private MetaTable() {
        super();
    }

    private static MetaTable ourInstance;
    public static MetaTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new MetaTable();
        }
        return ourInstance;
    }

    @Override
    protected Database getDatabase() {
        return EntityDatabase.getInstance();
    }

    // memory caches
    private final Map<ID, Meta> metaTable = new HashMap<>();

    private final Meta empty = new BaseMeta(new HashMap<>()) {

        @Override
        protected boolean hasSeed() {
            return false;
        }

        @Override
        public Address generateAddress(int type) {
            return null;
        }
    };

    //
    //  chat.dim.database.UserTable
    //

    @Override
    public boolean saveMeta(Meta meta, ID entity) {
        if (!MetaUtils.matches(entity, meta)) {
            Log.error("meta not match ID: " + entity + ", " + meta);
            return false;
        }
        // 0. check duplicate record
        Meta old = getMeta(entity);
        if (old != null) {
            // meta won't change, no need to update
            Log.info("meta already exists: " + entity);
            return true;
        }
        int type = MetaVersion.parseInt(meta.getType(), 0);
        String json = JSON.encode(meta.getPublicKey());
        String seed;
        byte[] fingerprint;
        if (MetaVersion.hasSeed(type)) {
            seed = meta.getSeed();
            fingerprint = meta.getFingerprint();
        } else {
            seed = "";
            fingerprint = null;
        }

        // 1. save into database
        ContentValues values = new ContentValues();
        values.put("did", entity.toString());
        values.put("version", type);
        values.put("pk", json);
        values.put("seed", seed);
        values.put("fingerprint", fingerprint);
        if (insert(EntityDatabase.T_META, null, values) < 0) {
            return false;
        }
        Log.info("-------- meta saved: " + entity);

        // 2. store into memory cache
        metaTable.put(entity, meta);
        return true;
    }

    @Override
    public Meta getMeta(ID entity) {
        // 1. try from memory cache
        Meta meta = metaTable.get(entity);
        if (meta == null) {
            // 2. try from database
            String[] columns = {"version", "pk", "seed", "fingerprint"};
            String[] selectionArgs = {entity.toString()};
            try (Cursor cursor = query(EntityDatabase.T_META, columns, "did=?", selectionArgs, null, null, null)) {
                if (cursor.moveToNext()) {
                    int type = cursor.getInt(0);
                    String json = cursor.getString(1);
                    PublicKey key = PublicKey.parse(JSON.decode(json));
                    if (MetaVersion.hasSeed(type)) {
                        String seed = cursor.getString(2);
                        byte[] fingerprint = cursor.getBlob(3);
                        TransportableData ted = TransportableData.create(EncodeAlgorithms.DEFAULT, fingerprint);
                        meta = Meta.create(Integer.toString(type), key, seed, ted);
                    } else {
                        meta = Meta.create(Integer.toString(type), key, null, null);
                    }
                    meta.put("version", type);  // compatible with 0.9.*
                }
            }
            if (meta == null) {
                // 2.1. place an empty meta for cache
                meta = empty;
            }

            // 3. store into memory cache
            metaTable.put(entity, meta);
        }
        if (meta == empty) {
            return null;
        }
        return meta;
    }
}
