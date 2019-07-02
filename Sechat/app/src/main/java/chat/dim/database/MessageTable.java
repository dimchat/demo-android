package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.common.Amanuensis;
import chat.dim.dkd.InstantMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;

public class MessageTable extends Database {

    private static Map<ID, List<InstantMessage>> chatHistory = new HashMap<>();
    private static List<ID> conversationList = new ArrayList<>();

    static {
        reloadData();
    }

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

    static void reloadData() {
        // TODO: scan 'message.js' in sub dirs of 'dkd'
        conversationList.add(ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj"));
        conversationList.add(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));

        sortConversations();
    }

    static void sortConversations() {
        // TODO: get last time of each conversations and sort
    }

    //---- conversations

    /**
     *  Get how many chat boxes
     *
     * @return conversations count
     */
    public static int numberOfConversations() {
        return conversationList.size();
    }

    /**
     *  Get chat box info
     *
     * @param index - sorted index
     * @return conversation instance
     */
    public static Conversation conversationAtIndex(int index) {
        ID identifier = conversationList.get(index);
        Amanuensis clerk = Amanuensis.getInstance();
        return clerk.getConversation(identifier);
    }

    /**
     *  Remove one chat box
     *
     * @param index - chat box index
     * @return false on error
     */
    public static ID removeConversationAtIndex(int index) {
        return conversationList.remove(index);
    }

    /**
     *  Remove the chat box
     *
     * @param chatBox - conversation instance
     * @return false on error
     */
    public static boolean removeConversation(Conversation chatBox) {
        ID identifier = chatBox.identifier;
        return conversationList.remove(identifier);
    }

    //-------- messages

    /**
     *  Get message count in this conversation for an entity
     *
     * @param chatBox - conversation instance
     * @return total count
     */
    public static int numberOfMessages(Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        return msgList == null ? 0 : msgList.size();
    }

    /**
     *  Get message at index of this conversation
     *
     * @param index - start from 0, latest first
     * @param chatBox - conversation instance
     * @return instant message
     */
    public static InstantMessage messageAtIndex(int index, Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        return msgList == null ? null : msgList.get(index);
    }

    /**
     *  Save the new message to local storage
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    public static boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        if (msgList == null) {
            msgList = new ArrayList<>();
            chatHistory.put(identifier, msgList);
        }
        return msgList.add(iMsg);
    }

    /**
     *  Delete the message
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    public static boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        ID identifier = chatBox.identifier;
        List<InstantMessage> msgList = chatHistory.get(identifier);
        return msgList != null && msgList.remove(iMsg);
    }

    /**
     *  Try to withdraw the message, maybe won't success
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    public static boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        // TODO: withdraw a message;
        return false;
    }
}
