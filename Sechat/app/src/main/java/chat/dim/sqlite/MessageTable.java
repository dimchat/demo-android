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
import chat.dim.crypto.SymmetricKey;
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
        try (Cursor cursor = query(MessageDatabase.T_MESSAGE, columns, null, null, "cid", null, null)) {
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
        clearCaches(identifier);
        String[] whereArgs = {identifier.toString()};
        delete(MessageDatabase.T_TRACE, "cid=?", whereArgs);
        return delete(MessageDatabase.T_MESSAGE, "cid=?", whereArgs) >= 0;
    }

    //-------- messages

    private ID cachedMessagesID = null;
    private List<InstantMessage<ID, SymmetricKey>> cachedMessages = null;

    private ID cachedTracesID = null;
    private Map<String, List<String>> cachedTraces = null;

    private void clearCaches(ID entity) {
        if (entity.equals(cachedMessagesID)) {
            cachedMessagesID = null;
            cachedMessages = null;
        }
        if (entity.equals(cachedTracesID)) {
            cachedTracesID = null;
            cachedTraces = null;
        }
        conversations = null;
    }

    private Map<String, List<String>> tracesInConversation(ID entity) {
        if (entity.equals(cachedTracesID) && cachedTraces != null) {
            return cachedTraces;
        }
        String[] columns = {"sn, signature, trace"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_TRACE, columns, "cid=?", selectionArgs, null, null, null)) {
            Map<String, List<String>> traces = new HashMap<>();
            List<String> array;
            int sn;
            String signature;
            String value;
            while (cursor.moveToNext()) {
                sn = cursor.getInt(0);
                signature = cursor.getString(1);
                value = cursor.getString(2);
                if (value == null) {
                    throw new NullPointerException("trace info empty: " + entity + ", sn=" + sn + ", signature=" + signature);
                }
                // traces by sn
                if (sn > 0) {
                    array = traces.get("" + sn);
                    if (array == null) {
                        array = new ArrayList<>();
                        traces.put("" + sn, array);
                    }
                    array.add(value);
                }
                // traces by signature
                if (signature != null && signature.length() > 0) {
                    array = traces.get(signature);
                    if (array == null) {
                        array = new ArrayList<>();
                        traces.put(signature, array);
                    }
                    array.add(value);
                }
            }
            cachedTraces = traces;
            cachedTracesID = entity;
            return traces;
        }
    }

    private List<String> getTraces(Map<String, List<String>> traces, int sn, String signature) {
        List<String> array1 = null, array2 = null;
        if (sn > 0) {
            array1 = traces.get("" + sn);
        }
        if (signature != null && signature.length() > 0) {
            array2 = traces.get(signature);
        }
        if (array1 == null || array1.size() == 0) {
            return array2;
        } else if (array2 == null || array2.size() == 0) {
            return array1;
        }
        List<String> array = new ArrayList<>(array1);
        for (String item: array2) {
            if (array.contains(item)) {
                continue;
            }
            array.add(item);
        }
        return array;
    }

    private List<InstantMessage<ID, SymmetricKey>> messagesInConversation(ID entity) {
        if (entity.equals(cachedMessagesID) && cachedMessages != null) {
            return cachedMessages;
        }
        Map<String, List<String>> traces = tracesInConversation(entity);
        if (traces == null) {
            traces = new HashMap<>();
        }
        String[] columns = {"sender", "receiver", "time", "content", "sn", "signature"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGE, columns, "cid=?", selectionArgs, null, null, null)) {
            List<InstantMessage<ID, SymmetricKey>> messages = new ArrayList<>();
            String sender;
            String receiver;
            long time;
            String content;
            int sn;
            String signature;
            InstantMessage<ID, SymmetricKey> iMsg;
            List<String> array;
            while (cursor.moveToNext()) {
                sender = cursor.getString(0);
                receiver = cursor.getString(1);
                time = cursor.getLong(2);
                content = cursor.getString(3);
                sn = cursor.getInt(4);
                signature = cursor.getString(5);
                iMsg = MessageDatabase.getInstanceMessage(sender, receiver, time, content);
                if (iMsg != null) {
                    // signature
                    if (signature != null && signature.length() > 0) {
                        iMsg.put("signature", signature);
                    }
                    // traces
                    array = getTraces(traces, sn, signature);
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
        try (Cursor cursor = query(MessageDatabase.T_MESSAGE, columns, "cid=?", selectionArgs, null, null, null)) {
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
        try (Cursor cursor = query(MessageDatabase.T_MESSAGE, columns, "cid=? AND read != 1", selectionArgs, null, null, null)) {
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
        return update(MessageDatabase.T_MESSAGE, values, "cid=? AND read != 1", whereArgs) >= 0;
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
        String[] columns = {"sender", "receiver", "time", "content", "signature"};
        String[] selectionArgs = {entity.toString()};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGE, columns, "cid=?", selectionArgs, null, null, "time DESC LIMIT 1")) {
            String sender;
            String receiver;
            long time;
            String content;
            String signature;
            InstantMessage<ID, SymmetricKey> iMsg;
            if (cursor.moveToNext()) {
                sender = cursor.getString(0);
                receiver = cursor.getString(1);
                time = cursor.getLong(2);
                content = cursor.getString(3);
                signature = cursor.getString(4);
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
        List<InstantMessage<ID, SymmetricKey>> messages = messagesInConversation(entity);
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
        int type = content.type;
        long sn = content.serialNumber;
        String signature = (String) iMsg.get("signature");
        if (signature == null) {
            signature = "";
        } else if (signature.length() > 8) {
            signature = signature.substring(0, 8);
        }

        // check for duplicated
        String[] columns = {"time"};
        String[] selectionArgs = {cid, sender, ""+sn};
        try (Cursor cursor = query(MessageDatabase.T_MESSAGE, columns, "cid=? AND sender=? AND sn=?", selectionArgs, null, null, null)) {
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
        byte[] data = JSON.encode(content);
        String text = new String(data, Charset.forName("UTF-8"));
        values.put("content", text);
        values.put("type", type);
        values.put("sn", sn);
        // extra
        if (signature.length() > 0) {
            values.put("signature", signature);
        }
        Object read = iMsg.get("read");
        if (read == null) {
            values.put("read", 0);
        } else {
            values.put("read", 1);
        }
        if (insert(MessageDatabase.T_MESSAGE, null, values) < 0) {
            return false;
        }
        // clear for reload
        clearCaches(entity);
        return true;
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, ID entity) {
        Object sender = iMsg.envelope.getSender();
        long sn = iMsg.getContent().serialNumber;
        String signature = (String) iMsg.get("signature");
        if (signature == null) {
            signature = "";
        }
        String[] whereArgs = {entity.toString(), sender.toString(), (sn > 0 ? ""+sn : "9527"), (signature.length() > 0 ? signature : "MOKY")};
        delete(MessageDatabase.T_TRACE, "cid=? AND sender=? AND (sn=? OR signature=?)", whereArgs);
        if (delete(MessageDatabase.T_MESSAGE, "cid=? AND sender=? AND (sn=? OR signature=?)", whereArgs) < 0) {
            return false;
        }
        // clear for reload
        clearCaches(entity);
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
        // FIXME: check for origin conversation
        Object sender = iMsg.envelope.getSender();
        Object receiver = iMsg.envelope.getReceiver();
        ReceiptCommand receipt = (ReceiptCommand) iMsg.getContent();
        if (entity.isUser()) {
            if (receiver.equals(receipt.get("sender"))) {
                receiver = receipt.get("receiver");
                if (receiver != null) {
                    entity = EntityDatabase.getID(receiver);
                }
            }
        }

        long sn = receipt.serialNumber;
        String signature = (String) receipt.get("signature");
        if (signature == null) {
            // only save receipt for normal content
            return false;
        } else if (signature.length() > 8) {
            signature = signature.substring(0, 8);
        }

        ContentValues values = new ContentValues();
        values.put("cid", entity.toString());
        values.put("sn", sn);
        values.put("signature", signature);
        values.put("trace", sender.toString());
        if (insert(MessageDatabase.T_TRACE, null, values) < 0) {
            return false;
        }

        // update message already loaded into memory cache
        if (entity.equals(cachedMessagesID) && cachedMessages != null) {
            InstantMessage<ID, SymmetricKey> item;
            int index = cachedMessages.size() - 1;
            for (; index >= 0; --index) {
                item = cachedMessages.get(index);
                if (!isMatch(item, receipt)) {
                    continue;
                }
                //noinspection unchecked
                List<String> traces = (List) item.get("traces");
                if (traces == null) {
                    traces = new ArrayList<>();
                    item.put("traces", traces);
                }
                // DISCUSS: what about the other fields 'sender', 'receiver', 'signature'
                //          in this receipt command?
                traces.add(sender.toString());
                break;
            }
        }
        return true;
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
