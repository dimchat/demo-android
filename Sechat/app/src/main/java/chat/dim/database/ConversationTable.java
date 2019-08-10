package chat.dim.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.ID;

public class ConversationTable extends ExternalStorage {

    private static List<ID> conversationList = new ArrayList<>();

    // "/sdcard/chat.dim.sechat/dkd/*"

    private static String getPath(ID user) {
        return root + "/dkd";
    }

    /**
     *  Refresh conversation list for current user
     *
     * @param user - user ID
     */
    static void reloadData(ID user) {
        // scan 'message.js' in sub dirs of 'dkd'
        String path = getPath(user);
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] items = dir.listFiles();
        if (items == null || items.length == 0) {
            return;
        }
        for (File file : items) {
            if (!file.isDirectory()) {
                continue;
            }
            if (!(new File(file.getPath(), "messages.js")).exists()) {
                continue;
            }
            conversationList.add(ID.getInstance(file.getPath()));
        }
        // sort with last message's time
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
