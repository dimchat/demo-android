package chat.dim.database;

import java.util.ArrayList;
import java.util.List;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;

public class ConversationTable extends Resource {

    private static List<ID> conversationList = new ArrayList<>();

    // "/sdcard/chat.dim.sechat/dkd/{address}/messages.js"

    private static String getMessageDirectory(Address address) {
        return publicDirectory + "/dkd/" + address;
    }
    private static String getMessageDirectory(ID identifier) {
        return getMessageDirectory(identifier.address);
    }

    /**
     *  Refresh conversation list for current user
     *
     * @param user - user ID
     */
    static void reloadData(ID user) {
        // TODO: scan 'message.js' in sub dirs of 'dkd'
        conversationList.add(ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj"));
        conversationList.add(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));

        sortConversations();
    }

    private static void sortConversations() {
        // TODO: get last time of each conversations and sort
    }

    //---- conversations

    public static int numberOfConversations() {
        return conversationList.size();
    }

    public static Conversation conversationAtIndex(int index) {
        ID identifier = conversationList.get(index);
        Amanuensis clerk = Amanuensis.getInstance();
        return clerk.getConversation(identifier);
    }

    public static ID removeConversationAtIndex(int index) {
        return conversationList.remove(index);
    }

    public static boolean removeConversation(Conversation chatBox) {
        ID identifier = chatBox.identifier;
        return conversationList.remove(identifier);
    }
}
