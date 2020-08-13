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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import chat.dim.ID;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;
import chat.dim.model.Facebook;

public class EntityDatabase extends SQLiteOpenHelper {

    private EntityDatabase(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }

    private static EntityDatabase ourInstance = null;
    public static void setContext(Context context) {
        ourInstance = new EntityDatabase(context, getDbName(), null, DB_VERSION);
    }
    static EntityDatabase getInstance() {
        assert ourInstance != null : "database should be initialized with context first";
        return ourInstance;
    }

    private static final String DB_NAME = "mkm.sqlite";
    private static final int DB_VERSION = 1;

    private static String getDbName() {
        return Paths.appendPathComponent(ExternalStorage.getRoot(), "db", DB_NAME);
    }

    static final String T_GROUP = "t_group";
    static final String T_MEMBERS = "t_members";

    //
    //  SQLiteOpenHelper
    //

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + T_GROUP + "(gid varchar(64), name varchar(32), founder varchar(64), owner varchar(64))");
        db.execSQL("create table " + T_MEMBERS + "(gid varchar(64), member varchar(64))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //
    //  MingKeMing
    //

    ID getID(String identifier) {
        Facebook facebook = Facebook.getInstance();
        return facebook.getID(identifier);
    }
}
