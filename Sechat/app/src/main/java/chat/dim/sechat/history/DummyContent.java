package chat.dim.sechat.history;

import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Profile;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent extends DummyList<DummyContent.Item> {

    private static Facebook facebook = Facebook.getInstance();
    private static Amanuensis clerk = Amanuensis.getInstance();
    private static ConversationDatabase msgDB = ConversationDatabase.getInstance();

    DummyContent() {
        super();
        reloadData();
    }

    public void reloadData() {
        clearItems();
        msgDB.reloadConversations();

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
        Comparator<Conversation> comparator = (chatBox1, chatBox2) -> {
            Date time1 = chatBox1.getLastTime();
            Date time2 = chatBox2.getLastTime();
            return time2.compareTo(time1);
        };
        Collections.sort(conversationList, comparator);
        // add
        for (int index = 0; index < count; index++) {
            chatBox = conversationList.get(index);
            addItem(new Item(chatBox));
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    class Item extends DummyItem {

        private final Conversation chatBox;

        Item(Conversation chatBox) {
            super();
            this.chatBox = chatBox;
        }

        ID getIdentifier() {
            return chatBox.identifier;
        }

        Uri getLogoUrl() {
            return SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher_foreground);
        }

        Uri getAvatarUrl() {
            ID identifier = getIdentifier();
            if (identifier != null) {
                String avatar = facebook.getAvatar(identifier);
                if (avatar != null) {
                    return Uri.parse(avatar);
                }
            }
            return SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher_round);
        }

        String getTitle() {
            ID identifier = chatBox.identifier;

            Profile profile = facebook.getProfile(identifier);
            String nickname = profile == null ? null : profile.getName();
            String username = identifier.name;
            if (nickname != null) {
                if (username != null && identifier.isUser()) {
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

        String getDesc() {
            String text = "(last message)";
            InstantMessage iMsg = chatBox.getLastVisibleMessage();
            if (iMsg != null) {
                text = msgDB.getContentText(iMsg.content);
            }
            return text;
        }
    }
}
