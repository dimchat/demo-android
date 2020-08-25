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

public class ANSDatabase extends Database {

    private ANSDatabase(Context context, String name, int version) {
        super(context, name, version);
    }

    private static ANSDatabase ourInstance = null;
    public static void setContext(Context context) {
        ourInstance = new ANSDatabase(context, getFilePath(DB_NAME), DB_VERSION);
    }
    static ANSDatabase getInstance() {
        assert ourInstance != null : "database should be initialized with context first";
        return ourInstance;
    }

    private static final String DB_NAME = "ans.db";
    private static final int DB_VERSION = 1;

    static final String T_RECORD = "t_record";

    @Override
    public void onCreate(SQLiteDatabase db) {
        // login info
        db.execSQL("CREATE TABLE " + T_RECORD + "(did VARCHAR(64), alias VARCHAR(32))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
