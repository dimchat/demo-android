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
package chat.dim.sqlite.key;

import android.content.ContentValues;
import android.database.Cursor;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.KeyFactory;
import chat.dim.crypto.PrivateKey;
import chat.dim.format.JSON;
import chat.dim.protocol.ID;
import chat.dim.sqlite.DataTable;

public final class PrivateKeyTable extends DataTable implements chat.dim.database.PrivateKeyTable {

    private PrivateKeyTable() {
        super(KeyDatabase.getInstance());
    }

    private static PrivateKeyTable ourInstance;
    public static PrivateKeyTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new PrivateKeyTable();
        }
        return ourInstance;
    }

    //
    //  chat.dim.database.PrivateKeyTable
    //

    @Override
    public boolean savePrivateKey(ID user, PrivateKey key, String type, int sign, int decrypt) {
        if (META.equals(type)/* && getPrivateKeyForSignature(user) != null*/) {
            String[] whereArgs = {user.toString(), META};
            delete(KeyDatabase.T_PRIVATE_KEY, "uid=? AND type=?", whereArgs);
        }
        byte[] data = JSON.encode(key);
        String text = new String(data, Charset.forName("UTF-8"));
        ContentValues values = new ContentValues();
        values.put("uid", user.toString());
        values.put("sk", text);
        values.put("type", type);
        values.put("sign", sign);
        values.put("decrypt", decrypt);
        return insert(KeyDatabase.T_PRIVATE_KEY, null, values) >= 0;
    }

    @Override
    public boolean savePrivateKey(ID user, PrivateKey key, String type) {
        if (key instanceof DecryptKey) {
            return savePrivateKey(user, key, type, 1, 1);
        } else {
            return savePrivateKey(user, key, type, 1, 0);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = null;
        String[] columns = {"sk"};
        String[] selectionArgs = {user.toString()};
        try (Cursor cursor = query(KeyDatabase.T_PRIVATE_KEY, columns,"uid=? AND type='M' AND sign=1", selectionArgs, null, null,"type DESC")) {
            String sk;
            Map<String, Object> info;
            if (cursor.moveToNext()) {
                sk = cursor.getString(0);
                info = (Map<String, Object>) JSON.decode(sk.getBytes(Charset.forName("UTF-8")));
                key = KeyFactory.getPrivateKey(info);
            }
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        List<DecryptKey> keys = new ArrayList<>();
        String[] columns = {"sk"};
        String[] selectionArgs = {user.toString()};
        try (Cursor cursor = query(KeyDatabase.T_PRIVATE_KEY, columns,"uid=? AND decrypt=1", selectionArgs, null, null,"type DESC")) {
            PrivateKey key;
            String sk;
            Map<String, Object> info;
            if (cursor.moveToNext()) {
                sk = cursor.getString(0);
                info = (Map<String, Object>) JSON.decode(sk.getBytes(Charset.forName("UTF-8")));
                key = KeyFactory.getPrivateKey(info);
                if (key instanceof DecryptKey) {
                    keys.add((DecryptKey) key);
                }
            }
        }
        return keys;
    }
}
