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
package chat.dim.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

import chat.dim.ID;
import chat.dim.database.AddressNameTable;

public class ANSTable extends DataTable implements AddressNameTable {

    private ANSTable() {
        super(ANSDatabase.getInstance());
        // fixed records
        if (getIdentifier("all") == null) {
            ID moky = ID.getInstance("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
            addRecord(moky, "founder");
            addRecord(ID.ANYONE, "owner");

            addRecord(ID.ANYONE, "anyone");
            addRecord(ID.ANYONE, ID.ANYONE.toString());
            addRecord(ID.EVERYONE, "everyone");
            addRecord(ID.EVERYONE, ID.EVERYONE.toString());
            addRecord(ID.EVERYONE, "all");
        }
    }

    private static ANSTable ourInstance;
    public static ANSTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new ANSTable();
        }
        return ourInstance;
    }

    //
    //  chat.dim.database.AddressNameTable
    //

    public boolean addRecord(ID identifier, String alias) {
        ContentValues values = new ContentValues();
        values.put("did", identifier.toString());
        if (getIdentifier(alias) == null) {
            // not exists, add new record
            values.put("alias", alias);
            return insert(ANSDatabase.T_RECORD, null, values) >= 0;
        } else {
            // update record
            String[] whereArgs = {alias};
            return update(ANSDatabase.T_RECORD, values, "alias=?", whereArgs) > 0;
        }
    }

    public ID getIdentifier(String alias) {
        ID identifier = null;
        String[] columns = {"did"};
        String[] selectionArgs = {alias};
        try (Cursor cursor = query(ANSDatabase.T_RECORD, columns, "alias=?", selectionArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                identifier = ID.getInstance(cursor.getString(0));
            }
        }
        return identifier;
    }
}
