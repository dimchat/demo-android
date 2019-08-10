package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.client.Conversation;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.ID;

public class MessageTable extends ExternalStorage {

    private static Map<ID, List<InstantMessage>> chatHistory = new HashMap<>();

    // "/sdcard/chat.dim.sechat/dkd/{address}/messages.js"

    private static String getMsgFilePath(ID entity) {
        return root + "/dkd/" + entity.address + "/messages.js";
    }

    private static List cacheMessages(Object array, ID entity) {
        if (!(array instanceof List)) {
            return null;
        }
        List list = (List) array;
        List<InstantMessage> messages = new ArrayList<>();
        for (Object msg : list) {
            messages.add(InstantMessage.getInstance(msg));
        }
        chatHistory.put(entity, messages);
        return messages;
    }

    private static List loadMessages(ID entity) {
        String path = getMsgFilePath(entity);
        try {
            Object array = readJSON(path);
            return cacheMessages(array, entity);
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
    }

    private static boolean saveMessages(ID entity) {
        List<InstantMessage> messages = chatHistory.get(entity);
        if (messages == null) {
            return false;
        }
        String path = getMsgFilePath(entity);
        try {
            return writeJSON(messages, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean removeMessages(ID entity) {
        String path = getMsgFilePath(entity);
        try {
            return delete(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean clearMessages(ID entity) {
        List<InstantMessage> messages = new ArrayList<>();
        String path = getMsgFilePath(entity);
        try {
            return writeJSON(messages, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //-------- messages

    public static int numberOfMessages(Conversation chatBox) {
        List<InstantMessage> msgList = chatHistory.get(chatBox.identifier);
        if (msgList == null) {
            return 0;
        }
        return msgList.size();
    }

    public static InstantMessage messageAtIndex(int index, Conversation chatBox) {
        List<InstantMessage> msgList = chatHistory.get(chatBox.identifier);
        assert msgList != null;
        return msgList.get(index);
    }

    public static boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        List<InstantMessage> msgList = chatHistory.get(chatBox.identifier);
        if (msgList == null) {
            msgList = new ArrayList<>();
            chatHistory.put(chatBox.identifier, msgList);
        }
        msgList.add(iMsg);
        return saveMessages(chatBox.identifier);
    }

    public static boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        List<InstantMessage> msgList = chatHistory.get(chatBox.identifier);
        assert msgList != null;
        msgList.remove(iMsg);
        return saveMessages(chatBox.identifier);
    }

    public static boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        // TODO: withdraw a message;
        return false;
    }

    public static boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        // TODO: save receipt of instant message
        return false;
    }

    public static boolean removeMessages(Conversation chatBox) {
        return removeMessages(chatBox.identifier);
    }

    public static boolean clearMessages(Conversation chatBox) {
        return clearMessages(chatBox.identifier);
    }
}
