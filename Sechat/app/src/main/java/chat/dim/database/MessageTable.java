package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.client.Conversation;
import chat.dim.dkd.InstantMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;

public class MessageTable extends Resource {

    private static Map<ID, List<InstantMessage>> chatHistory = new HashMap<>();

    // "/sdcard/chat.dim.sechat/dkd/{address}/messages.js"

    private static String getMessageDirectory(Address address) {
        return publicDirectory + "/dkd/" + address;
    }
    private static String getMessageDirectory(ID identifier) {
        return getMessageDirectory(identifier.address);
    }

    /**
     *  Load messages for entity ID
     *
     * @param identifier - contact/group ID
     * @return message list
     */
    @SuppressWarnings("unchecked")
    static List<InstantMessage> loadMessages(ID identifier) {
        String dir = getMessageDirectory(identifier);
        try {
            String json = readTextFile("messages.js", dir);
            return (List<InstantMessage>) JSON.parser.decode(json);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Save messages for entity ID
     *
     * @param messages - message list
     * @param identifier - contact/group ID
     * @return true on success
     */
    static boolean saveMessages(List<InstantMessage> messages, ID identifier) {
        String dir = getMessageDirectory(identifier.address);
        try {
            return writeJSONFile(messages, "messages.js", dir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *  Remove all messages of entity
     *
     * @param identifier - entity ID
     * @return true on success
     */
    static boolean removeMessages(ID identifier) {
        String dir = getMessageDirectory(identifier);
        return removeFile("messages.js", dir);
    }

    /**
     *  Empty messages of entity
     *
     * @param identifier - entity ID
     * @return true on success
     */
    static boolean clearMessages(ID identifier) {
        List<InstantMessage> messages = new ArrayList<>();
        String dir = getMessageDirectory(identifier);
        try {
            return writeJSONFile(messages, "messages.js", dir);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //-------- messages

    public static int numberOfMessages(Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        return msgList == null ? 0 : msgList.size();
    }

    public static InstantMessage messageAtIndex(int index, Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        return msgList == null ? null : msgList.get(index);
    }

    public static boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        if (msgList == null) {
            msgList = new ArrayList<>();
            chatHistory.put(identifier, msgList);
        }
        return msgList.add(iMsg);
    }

    public static boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        return msgList != null && msgList.remove(iMsg);
    }

    public static boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        // TODO: withdraw a message;
        return false;
    }

    public static boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        // TODO: save receipt of instant message
        return false;
    }
}
