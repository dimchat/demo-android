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
package chat.dim.sqlite.dkd;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Map;

import chat.dim.Messenger;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.protocol.InstantMessage;
import chat.dim.sqlite.Database;

public final class MessageDatabase extends Database {

    private MessageDatabase(Context context, String name, int version) {
        super(context, name, version);
    }

    private static MessageDatabase ourInstance = null;

    public static Messenger messenger = null;

    public static void setContext(Context context) {
        ourInstance = new MessageDatabase(context, getFilePath(DB_NAME), DB_VERSION);
    }

    static MessageDatabase getInstance() {
        assert ourInstance != null : "database should be initialized with context first";
        return ourInstance;
    }

    private static final String DB_NAME = "dkd.db";
    private static final int DB_VERSION = 1;

    static final String T_MESSAGE = "t_message";
    static final String T_TRACE = "t_trace";

    //
    //  SQLiteOpenHelper
    //

    @Override
    public void onCreate(SQLiteDatabase db) {
        // messages
        db.execSQL("CREATE TABLE " + T_MESSAGE + "(cid VARCHAR(64), sender VARCHAR(64), receiver VARCHAR(64), time INTEGER," +
                // content info
                " content TEXT, type INTEGER, sn VARCHAR(20)," +
                // extra info
                " signature VARCHAR(8), read BIT)");
        db.execSQL("CREATE INDEX cid_index ON " + T_MESSAGE + "(cid)");

        // traces for messages
        db.execSQL("CREATE TABLE " + T_TRACE + "(cid VARCHAR(64), sn VARCHAR(20), signature VARCHAR(8), trace TEXT)");
        db.execSQL("CREATE INDEX trace_id_index ON " + T_TRACE + "(cid)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    //
    //  DaoKeDao
    //

    static InstantMessage getInstanceMessage(String sender, String receiver, long timestamp, String content) {
        Map dict = (Map) JSON.decode(UTF8.encode(content));
        if (dict == null) {
            throw new NullPointerException("message content error: " + content);
        }
        Map<String, Object> msg = new HashMap<>();
        msg.put("sender", sender);
        msg.put("receiver", receiver);
        msg.put("time", timestamp);
        msg.put("content", dict);
        return getInstanceMessage(msg);
    }

    @SuppressWarnings("unchecked")
    private static InstantMessage getInstanceMessage(Map msg) {
        InstantMessage iMsg = InstantMessage.parse(msg);
        if (iMsg != null) {
            iMsg.setDelegate(messenger);
        }
        return iMsg;
    }
}
