package chat.dim.sechat.conversations.dummy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Profile;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent {

    private static Facebook facebook = Facebook.getInstance();
    private static Amanuensis clerk = Amanuensis.getInstance();
    private static ConversationDatabase msgDB = ConversationDatabase.getInstance();

    /**
     * An array of sample (dummy) items.
     */
    public static final List<DummyItem> ITEMS = new ArrayList<DummyItem>();

    /**
     * A map of sample (dummy) items, by ID.
     */
    public static final Map<Object, DummyItem> ITEM_MAP = new HashMap<>();

    static {
        reloadData();
    }

    public static void reloadData() {
        ITEMS.clear();

        List<Conversation> conversationList = new ArrayList<>();
        Conversation chatBox;
        // load
        int count = msgDB.numberOfConversations();
        ID identifier;
        for (int index = 0; index < count; index++) {
            identifier = msgDB.conversationAtIndex(index);
            chatBox = clerk.getConversation(identifier);
            if (chatBox == null) {
                throw new NullPointerException("failed to create chat box: " + identifier);
            }
            conversationList.add(chatBox);
        }
        // sort
        Comparator<Conversation> comparator = new Comparator<Conversation>() {
            @Override
            public int compare(Conversation chatBox1, Conversation chatBox2) {
                Date time1 = chatBox1.getLastTime();
                Date time2 = chatBox2.getLastTime();
                return time2.compareTo(time1);
            }
        };
        Collections.sort(conversationList, comparator);
        // add
        for (int index = 0; index < count; index++) {
            chatBox = conversationList.get(index);
            addItem(new DummyItem(chatBox));
        }
    }

    private static void addItem(DummyItem item) {
        ITEMS.add(item);
        ITEM_MAP.put(item.getIdentifier(), item);
    }

    private static String makeDetails(int position) {
        StringBuilder builder = new StringBuilder();
        builder.append("Details about Item: ").append(position);
        for (int i = 0; i < position; i++) {
            builder.append("\nMore details information here.");
        }
        return builder.toString();
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class DummyItem {

        private final Conversation chatBox;

        DummyItem(Conversation chatBox) {
            this.chatBox = chatBox;
        }

        public ID getIdentifier() {
            return chatBox.identifier;
        }

        public String getTitle() {
            ID identifier = chatBox.identifier;

            Profile profile = facebook.getProfile(identifier);
            String nickname = profile == null ? null : profile.getName();
            String username = identifier.name;
            if (nickname != null) {
                if (username != null && identifier.getType().isUser()) {
                    return nickname + " (" + username + ")";
                }
                return nickname;
            } else if (username != null) {
                return username;
            } else {
                // BTC address
                return identifier.address.toString();
            }
        }

        public String getDesc() {
            String text = "(last message)";
            InstantMessage iMsg = chatBox.getLastVisibleMessage();
            if (iMsg != null) {
                text = msgDB.getContentText(iMsg.content);
            }
            return text;
        }
    }
}
