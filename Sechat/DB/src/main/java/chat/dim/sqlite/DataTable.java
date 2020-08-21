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
import android.database.sqlite.SQLiteDatabase;

public class DataTable {

    private final Database database;

    protected DataTable(Database db) {
        super();
        database = db;
    }

    long insert(String table, String nullColumnHack, ContentValues values) {
        SQLiteDatabase db = database.getWritableDatabase();
        if (db == null) {
            throw new NullPointerException("failed to get writable database");
        }
        return db.insert(table, nullColumnHack, values);
    }

    int delete(String table, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = database.getWritableDatabase();
        if (db == null) {
            throw new NullPointerException("failed to get writable database");
        }
        return db.delete(table, whereClause, whereArgs);
    }

    int update(String table, ContentValues values, String whereClause, String[] whereArgs) {
        SQLiteDatabase db = database.getWritableDatabase();
        if (db == null) {
            throw new NullPointerException("failed to get writable database");
        }
        return db.update(table, values, whereClause, whereArgs);
    }

    Cursor query(String table, String[] columns, String selection,
                 String[] selectionArgs, String groupBy, String having,
                 String orderBy) {
        SQLiteDatabase db = database.getReadableDatabase();
        if (db == null) {
            throw new NullPointerException("failed to get readable database");
        }
        return db.query(table, columns, selection, selectionArgs, groupBy, having, orderBy);
    }
}
