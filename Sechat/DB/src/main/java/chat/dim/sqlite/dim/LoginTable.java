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
package chat.dim.sqlite.dim;

import android.content.ContentValues;
import android.database.Cursor;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;

import chat.dim.ID;
import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.format.JSON;
import chat.dim.protocol.LoginCommand;
import chat.dim.sqlite.DataTable;

public class LoginTable extends DataTable implements LoginCommandProcessor.DataHandler {

    private LoginTable() {
        super(MainDatabase.getInstance());
    }

    private static LoginTable ourInstance;
    public static LoginTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new LoginTable();
        }
        return ourInstance;
    }

    //
    //  access for "LOGIN" command
    //

    @SuppressWarnings("unchecked")
    public LoginCommand getLoginCommand(ID user) {
        String[] columns = {"command"};
        String[] selectionArgs = {user.toString()};
        try (Cursor cursor = query(MainDatabase.T_LOGIN, columns, "uid=?", selectionArgs, null, null, null)) {
            String text;
            Object info;
            if (cursor.moveToNext()) {
                text = cursor.getString(0);
                info = JSON.decode(text.getBytes(Charset.forName("UTF-8")));
                if (info instanceof Map) {
                    return new LoginCommand((Map<String, Object>) info);
                }
            }
        }
        return null;
    }

    @Override
    public boolean save(LoginCommand command) {
        ID user = command.getIdentifier();
        Date oldTime = getLoginTime(user);
        Map<String, Object> station = command.getStation();
        String sid = (String) station.get("ID");
        String text = new String(JSON.encode(command), Charset.forName("UTF-8"));

        ContentValues values = new ContentValues();
        values.put("station", sid);
        values.put("command", text);
        if (oldTime == null) {
            // not exists, insert new record
            values.put("uid", user.toString());
            values.put("time", command.time.getTime() / 1000);
            return insert(MainDatabase.T_LOGIN, null, values) >= 0;
        } else {
            // update record with user ID
            if (oldTime.after(command.time)) {
                values.put("time", oldTime.getTime() / 1000);
            } else {
                values.put("time", command.time.getTime() / 1000);
            }
            String[] whereArgs = {user.toString()};
            return update(MainDatabase.T_LOGIN, values, "uid=?", whereArgs) > 0;
        }
    }

    public Date getLoginTime(ID user) {
        String[] columns = {"time"};
        String[] selectionArgs = {user.toString()};
        try (Cursor cursor = query(MainDatabase.T_LOGIN, columns, "uid=?", selectionArgs, null, null, null)) {
            long time;
            if (cursor.moveToNext()) {
                time = cursor.getLong(0);
                return new Date(time * 1000);
            }
        }
        return null;
    }

    public boolean update(ID user, Date time) {
        Date oldTime = getLoginTime(user);

        ContentValues values = new ContentValues();
        values.put("time", time.getTime() / 1000);
        if (oldTime == null) {
            // not exists, insert new record
            values.put("uid", user.toString());
            return insert(MainDatabase.T_LOGIN, null, values) >= 0;
        } else if (oldTime.after(time)) {
            // error
            return false;
        } else {
            String[] whereArgs = {user.toString()};
            return update(MainDatabase.T_LOGIN, values, "uid=?", whereArgs) > 0;
        }
    }
}
