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
import java.util.Map;

import chat.dim.ID;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.sqlite.DataTable;

public class MsgKeyTable extends DataTable implements chat.dim.database.MsgKeyTable {

    private MsgKeyTable() {
        super(KeyDatabase.getInstance());
    }

    private static MsgKeyTable ourInstance;
    public static MsgKeyTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new MsgKeyTable();
        }
        return ourInstance;
    }

    //
    //  chat.dim.database.PrivateKeyTable
    //

    @SuppressWarnings("unchecked")
    @Override
    public SymmetricKey getKey(ID from, ID to) {
        SymmetricKey key = null;
        String[] columns = {"pwd"};
        String[] selectionArgs = {from.toString(), to.toString()};
        try (Cursor cursor = query(KeyDatabase.T_MESSAGE_KEY, columns,"sender=? AND receiver=?", selectionArgs, null, null,null)) {
            String sk;
            Map<String, Object> info;
            if (cursor.moveToNext()) {
                sk = cursor.getString(0);
                info = (Map<String, Object>) JSON.decode(sk.getBytes(Charset.forName("UTF-8")));
                key = SymmetricKey.getInstance(info);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return key;
    }

    @Override
    public boolean addKey(ID from, ID to, SymmetricKey key) {
        String[] whereArgs = {from.toString(), to.toString()};
        delete(KeyDatabase.T_MESSAGE_KEY, "sender=? AND receiver=?", whereArgs);

        byte[] data = JSON.encode(key);
        String text = new String(data, Charset.forName("UTF-8"));
        ContentValues values = new ContentValues();
        values.put("sender", from.toString());
        values.put("receiver", to.toString());
        values.put("pwd", text);
        return insert(KeyDatabase.T_MESSAGE_KEY, null, values) >= 0;
    }
}
