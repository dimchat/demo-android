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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.format.JSON;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.utils.Log;

public class MessageTable extends DataTable implements chat.dim.database.MessageTable {

    private MessageTable() {
        super(MessageDatabase.getInstance());
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
        String[] columns = {"cid"};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGES, columns, null, null, "cid", null, null)) {
            // get conversation IDs
            List<ID> array = new ArrayList<>();
            ID identifier;
            while (cursor.moveToNext()) {
                identifier = EntityDatabase.getID(cursor.getString(0));
                if (identifier != null) {
                    array.add(identifier);
                }
            }
            // sort by last message time
            Comparator<ID> comparator = (cid1, cid2) -> {
                Date time1, time2;
                InstantMessage msg1 = lastMessage(cid1);
                if (msg1 == null) {
                    time1 = new Date();
                } else {
                    time1 = msg1.envelope.getTime();
                }
                InstantMessage msg2 = lastMessage(cid2);
                if (msg2 == null) {
                    time2 = new Date();
                } else {
                    time2 = msg2.envelope.getTime();
                }
                return time2.compareTo(time1);
            };
            Collections.sort(array, comparator);

            conversations = array;
            return conversations;
        }
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
        conversations = null;
        String[] whereArgs = {identifier.toString()};
        delete(MessageDatabase.T_TRACES, "cid=?", whereArgs);
        return delete(MessageDatabase.T_MESSAGES, "cid=?", whereArgs) >= 0;
    }

    //-------- messages

    private ID cachedMessagesID = null;
    private List<InstantMessage> cachedMessages = null;

    private ID cachedTracesID = null;
    private Map<String, List<Map>> cachedTraces = null;

    private Map<String, List<Map>> tracesInConversation(ID entity) {
        if (entity.equals(cachedTracesID) && cachedTraces != null) {
            return cachedTraces;
        }
        String[] columns = {"sender, sn, trace"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_TRACES, columns, "cid=?", selectionArgs, null, null, null)) {
            Map<String, List<Map>> traces = new HashMap<>();
            List<Map> array;
            String sender;
            String sn;
            String key;
            String value;
            Map receipt;
            while (cursor.moveToNext()) {
                sender = cursor.getString(0);
                sn = cursor.getString(1);
                key = sender + "," + sn;
                value = cursor.getString(2);
                if (value == null) {
                    throw new NullPointerException("trace info empty: " + sender + ", " + sn);
                }
                receipt = (Map) JSON.decode(value.getBytes(Charset.forName("UTF-8")));
                array = traces.get(key);
                if (array == null) {
                    array = new ArrayList<>();
                    traces.put(key, array);
                }
                array.add(receipt);
            }
            cachedTraces = traces;
            cachedTracesID = entity;
            return traces;
        }
    }

    public List<InstantMessage> messagesInConversation(ID entity) {
        if (entity.equals(cachedMessagesID) && cachedMessages != null) {
            return cachedMessages;
        }
        Map<String, List<Map>> traces = tracesInConversation(entity);
        if (traces == null) {
            traces = new HashMap<>();
        }
        String[] columns = {"sender", "receiver", "time", "sn", "content", "signature"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGES, columns, "cid=?", selectionArgs, null, null, null)) {
            List<InstantMessage> messages = new ArrayList<>();
            String sender;
            String receiver;
            long time;
            String sn;
            String content;
            String signature;
            InstantMessage iMsg;
            List<Map> array;
            while (cursor.moveToNext()) {
                sender = cursor.getString(0);
                receiver = cursor.getString(1);
                time = cursor.getLong(2);
                sn = cursor.getString(3);
                content = cursor.getString(4);
                signature = cursor.getString(5);
                iMsg = MessageDatabase.getInstanceMessage(sender, receiver, time, content);
                if (iMsg != null) {
                    // signature
                    if (signature != null && signature.length() > 0) {
                        iMsg.put("signature", signature);
                    }
                    // traces
                    array = traces.get(sender + "," + sn);
                    if (array != null && array.size() > 0) {
                        iMsg.put("traces", array);
                    }
                    messages.add(iMsg);
                }
            }
            cachedMessages = messages;
            cachedMessagesID = entity;
            return messages;
        }
    }

    @Override
    public int numberOfMessages(ID entity) {
        if (entity.equals(cachedMessagesID) &&  cachedMessages != null) {
            return cachedMessages.size();
        }
        String[] columns = {"COUNT(*)"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGES, columns, "cid=?", selectionArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
            return 0;
        }
    }

    @Override
    public int numberOfUnreadMessages(ID entity) {
        String[] columns = {"COUNT(*)"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGES, columns, "cid=? AND read != 1", selectionArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                return cursor.getInt(0);
            }
            return 0;
        }
    }

    @Override
    public boolean clearUnreadMessages(ID entity) {
        ContentValues values = new ContentValues();
        values.put("read", 1);
        String[] whereArgs = {entity.toString()};
        return update(MessageDatabase.T_MESSAGES, values, "cid=?", whereArgs) >= 0;
    }

    @Override
    public InstantMessage lastMessage(ID entity) {
        if (entity.equals(cachedMessagesID) && cachedMessages != null) {
            int count = cachedMessages.size();
            if (count > 0) {
                return cachedMessages.get(count - 1);
            }
            return null;
        }
        String[] columns = {"sender", "receiver", "time", "sn", "content", "signature"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGES, columns, "cid=?", selectionArgs, null, null, "time DESC LIMIT 1")) {
            String sender;
            String receiver;
            long time;
            String sn;
            String content;
            String signature;
            InstantMessage iMsg;
            List<Map> array;
            if (cursor.moveToNext()) {
                sender = cursor.getString(0);
                receiver = cursor.getString(1);
                time = cursor.getLong(2);
                sn = cursor.getString(3);
                content = cursor.getString(4);
                signature = cursor.getString(5);
                iMsg = MessageDatabase.getInstanceMessage(sender, receiver, time, content);
                if (iMsg != null) {
                    // signature
                    if (signature != null && signature.length() > 0) {
                        iMsg.put("signature", signature);
                    }
                    return iMsg;
                }
            }
            return null;
        }
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
        Content content = iMsg.getContent();
        if (content == null) {
            return false;
        }
        String cid = entity.toString();
        String sender = iMsg.envelope.getSender().toString();
        String receiver = iMsg.envelope.getReceiver().toString();
        Date time = iMsg.envelope.getTime();
        long sn = content.serialNumber;
        int type = content.type;

        // check for duplicated
        String[] columns = {"sender", "receiver", "time", "sn", "content", "signature"};
        String[] selectionArgs = {cid, sender, ""+sn};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGES, columns, "cid=? AND sender=? AND sn=?", selectionArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                // record exists
                Log.info("drop duplicated msg: " + iMsg);
                return false;
            }
        }

        ContentValues values = new ContentValues();
        values.put("cid", cid);
        // envelope
        values.put("sender", sender);
        values.put("receiver", receiver);
        values.put("time", time.getTime() / 1000);
        // content
        values.put("sn", sn);
        values.put("type", type);
        byte[] data = JSON.encode(content);
        String text = new String(data, Charset.forName("UTF-8"));
        values.put("content", text);
        if (insert(MessageDatabase.T_MESSAGES, null, values) < 0) {
            return false;
        }
        // clear for reload
        cachedMessages = null;
        cachedMessagesID = null;
        conversations = null;
        return true;
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, ID entity) {
        Object sender = iMsg.envelope.getSender();
        long sn = iMsg.getContent().serialNumber;
        String[] whereArgs = {entity.toString(), sender.toString(), "" + sn};
        delete(MessageDatabase.T_TRACES, "cid=? AND sender=? AND sn=?", whereArgs);
        if (delete(MessageDatabase.T_MESSAGES, "cid=? AND sender=? AND sn=?", whereArgs) < 0) {
            return false;
        }
        // clear for reload
        cachedMessages = null;
        cachedMessagesID = null;
        conversations = null;
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
        if (insert(MessageDatabase.T_TRACES, null, values) < 0) {
            return false;
        }

        // update message already loaded into memory cache
        if (entity.equals(cachedMessagesID) && cachedMessages != null) {
            InstantMessage item;
            int index = cachedMessages.size() - 1;
            for (; index >= 0; --index) {
                item = cachedMessages.get(index);
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
