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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.format.JSON;
import chat.dim.protocol.ReceiptCommand;

public class MessageTable implements chat.dim.database.MessageTable {

    private final MessageDatabase messageDatabase;

    private MessageTable() {
        super();
        messageDatabase = MessageDatabase.getInstance();
    }

    private static MessageTable ourInstance;
    public static MessageTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new MessageTable();
        }
        return ourInstance;
    }

    //
    //  chat.dim.database.MessageTable
    //

    //---- conversations

    private List<ID> conversations = null;

    private List<ID> allConversations() {
        if (conversations != null) {
            return conversations;
        }
        SQLiteDatabase db = messageDatabase.getReadableDatabase();
        if (db == null) {
            return null;
        }
        String[] columns = {"cid"};
        try (Cursor cursor = db.query(MessageDatabase.T_MESSAGES, columns, null, null, "cid", null, null)) {
            conversations = new ArrayList<>();
            ID identifier;
            while (cursor.moveToNext()) {
                identifier = EntityDatabase.getID(cursor.getString(0));
                if (identifier != null) {
                    conversations.add(identifier);
                }
            }
            sortConversations();
            return conversations;
        }
    }

    private void sortConversations() {
        // TODO: sort by last message time
    }

    @Override
    public int numberOfConversations() {
        List<ID> array = allConversations();
        if (array == null) {
            return 0;
        }
        return array.size();
    }

    @Override
    public ID conversationAtIndex(int index) {
        List<ID> array = allConversations();
        if (array == null || array.size() <= index) {
            return null;
        }
        return array.get(index);
    }

    @Override
    public boolean removeConversationAtIndex(int index) {
        ID identifier = conversationAtIndex(index);
        if (identifier == null) {
            return false;
        }
        return removeConversation(identifier);
    }

    @Override
    public boolean removeConversation(ID identifier) {
        return removeMessages(identifier);
    }

    //-------- messages

    private Map<ID, List<InstantMessage>> messageCaches = new HashMap<>();
    private Map<ID, Map<String, Map>> tracesCaches = new HashMap<>();

    private Map<String, Map> tracesInConversation(ID entity) {
        Map<String, Map> traces = tracesCaches.get(entity);
        if (traces != null) {
            return traces;
        }
        SQLiteDatabase db = messageDatabase.getReadableDatabase();
        if (db == null) {
            return null;
        }
        String[] columns = {"sender, sn, trace"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = db.query(MessageDatabase.T_TRACES, columns, "cid=?", selectionArgs, null, null, null)) {
            traces = new HashMap<>();
            String sender;
            String sn;
            String value;
            Map receipt;
            while (cursor.moveToNext()) {
                sender = cursor.getString(0);
                sn = cursor.getString(1);
                value = cursor.getString(2);
                if (value != null) {
                    receipt = (Map) JSON.decode(value.getBytes(Charset.forName("UTF-8")));
                    traces.put(sender + "," + sn, receipt);
                }
            }
            tracesCaches.put(entity, traces);
            return traces;
        }
    }

    @Override
    public List<InstantMessage> messagesInConversation(ID entity) {
        List<InstantMessage> messages = messageCaches.get(entity);
        if (messages != null) {
            return messages;
        }
        SQLiteDatabase db = messageDatabase.getReadableDatabase();
        if (db == null) {
            return null;
        }
        String[] columns = {"sender", "receiver", "time", "content"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = db.query(MessageDatabase.T_MESSAGES, columns, "cid=?", selectionArgs, null, null, null)) {
            messages = new ArrayList<>();
            String sender;
            String receiver;
            long time;
            String content;
            InstantMessage iMsg;
            while (cursor.moveToNext()) {
                sender = cursor.getString(0);
                receiver = cursor.getString(1);
                time = cursor.getLong(2);
                content = cursor.getString(3);
                iMsg = MessageDatabase.getInstanceMessage(sender, receiver, time, content);
                if (iMsg != null) {
                    messages.add(iMsg);
                }
            }
            messageCaches.put(entity, messages);
            return messages;
        }
    }

    @Override
    public int numberOfMessages(ID entity) {
        List<InstantMessage> messages = messageCaches.get(entity);
        if (messages != null) {
            return messages.size();
        }
        SQLiteDatabase db = messageDatabase.getReadableDatabase();
        if (db == null) {
            return 0;
        }
        String[] columns = {"COUNT(*)"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = db.query(MessageDatabase.T_MESSAGES, columns, "cid=?", selectionArgs, "cid", null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
            return 0;
        }
    }

    @Override
    public int numberOfUnreadMessages(ID entity) {
        SQLiteDatabase db = messageDatabase.getReadableDatabase();
        if (db == null) {
            return 0;
        }
        String[] columns = {"COUNT(*)"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = db.query(MessageDatabase.T_MESSAGES, columns, "cid=? AND read != 1", selectionArgs, "cid", null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
            return 0;
        }
    }

    @Override
    public boolean clearUnreadMessages(ID entity) {
        SQLiteDatabase db = messageDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("read", 1);
        String[] whereArgs = {entity.toString()};
        return db.update(MessageDatabase.T_MESSAGES, values, "cid=?", whereArgs) >= 0;
    }

    @Override
    public InstantMessage messageAtIndex(int index, ID entity) {
        List<InstantMessage> messages = messagesInConversation(entity);
        if (messages == null || messages.size() <= index) {
            return null;
        }
        return messages.get(index);
    }

    @Override
    public boolean insertMessage(InstantMessage iMsg, ID entity) {
        SQLiteDatabase db = messageDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("cid", entity.toString());
        // envelope
        values.put("sender", iMsg.envelope.getSender().toString());
        values.put("receiver", iMsg.envelope.getReceiver().toString());
        values.put("time", iMsg.envelope.getTime().getTime() / 1000);
        // content
        Content content = iMsg.getContent();
        values.put("sn", content.serialNumber);
        values.put("type", content.type);
        byte[] data = JSON.encode(content);
        String text = new String(data, Charset.forName("UTF-8"));
        values.put("content", text);
        if (db.insert(MessageDatabase.T_MESSAGES, null, values) < 0) {
            return false;
        }
        // clear for reload
        messageCaches.remove(entity);
        return true;
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, ID entity) {
        SQLiteDatabase db = messageDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }
        Object sender = iMsg.envelope.getSender();
        long sn = iMsg.getContent().serialNumber;
        String[] whereArgs = {entity.toString(), sender.toString(), "" + sn};
        db.delete(MessageDatabase.T_TRACES, "cid=? AND sender=? AND sn=?", whereArgs);
        if (db.delete(MessageDatabase.T_MESSAGES, "cid=? AND sender=? AND sn=?", whereArgs) < 0) {
            return false;
        }
        // clear for reload
        messageCaches.remove(entity);
        return true;
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, ID entity) {
        return false;
    }

    @Override
    public boolean saveReceipt(InstantMessage iMsg, ID entity) {
        // save receipt of instant message
        if (!(iMsg.getContent() instanceof ReceiptCommand)) {
            return false;
        }
        SQLiteDatabase db = messageDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }

        Object sender = iMsg.envelope.getSender();
        ReceiptCommand receipt = (ReceiptCommand) iMsg.getContent();
        long sn = receipt.serialNumber;
        byte[] data = JSON.encode(receipt);
        String json = new String(data, Charset.forName("UTF-8"));

        ContentValues values = new ContentValues();
        values.put("cid", entity.toString());
        values.put("sender", sender.toString());
        values.put("sn", "" + sn);
        values.put("trace", json);
        if (db.insert(MessageDatabase.T_TRACES, null, values) < 0) {
            return false;
        }

        List<InstantMessage> messages = messageCaches.get(entity);
        if (messages != null) {
            // update message already loaded into memory cache
            InstantMessage item;
            int index = messages.size() - 1;
            for (; index >= 0; --index) {
                item = messages.get(index);
                if (!isMatch(item, receipt)) {
                    continue;
                }
                //noinspection unchecked
                List<Object> traces = (List) item.get("traces");
                if (traces == null) {
                    traces = new ArrayList<>();
                    item.put("traces", traces);
                }
                // DISCUSS: what about the other fields 'sender', 'receiver', 'signature'
                //          in this receipt command?
                traces.add(receipt);
                break;
            }
        }
        return false;
    }

    @Override
    public boolean removeMessages(ID entity) {
        SQLiteDatabase db = messageDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }
        String[] whereArgs = {entity.toString()};
        db.delete(MessageDatabase.T_TRACES, "cid=?", whereArgs);
        return db.delete(MessageDatabase.T_MESSAGES, "cid=?", whereArgs) >= 0;
    }

    private static boolean isMatch(InstantMessage iMsg, ReceiptCommand receipt) {

        // 1. check sender & receiver
        Object sender = receipt.get("sender");
        if (sender != null && !iMsg.envelope.getSender().equals(sender)) {
            return false;
        }
        Object receiver = receipt.get("receiver");
        if (receiver != null && !iMsg.envelope.getReceiver().equals(receiver)) {
            return false;
        }
        // 2. check signature
        String sig1 = (String) receipt.get("signature");
        String sig2 = (String) iMsg.get("signature");
        if (sig1 != null && sig2 != null) {
            sig1 = sig1.replaceAll("\n", "");
            sig2 = sig2.replaceAll("\n", "");
            int len1 = sig1.length();
            int len2 = sig2.length();
            if (len1 > len2) {
                return sig1.contains(sig2);
            } else if (len1 < len2) {
                return sig2.contains(sig1);
            } else {
                return sig1.equals(sig2);
            }
        }
        // 3. check sn
        return iMsg.getContent().serialNumber == receipt.serialNumber;
    }
}
