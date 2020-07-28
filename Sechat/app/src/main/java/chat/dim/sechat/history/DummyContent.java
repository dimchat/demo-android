package chat.dim.sechat.history;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class DummyContent extends DummyList<DummyContent.Item> {

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
    static class Item extends DummyItem {

        private final Conversation chatBox;

        Item(Conversation chatBox) {
            super();
            this.chatBox = chatBox;
        }

        ID getIdentifier() {
            return chatBox.identifier;
        }

        Uri getLogoUri() {
            return GroupViewModel.getLogoUri(chatBox.identifier);
        }

        Uri getAvatarUri() {
            return UserViewModel.getAvatarUri(chatBox.identifier);
        }

        String getTitle() {
            if (chatBox.identifier.isGroup()) {
                return EntityViewModel.getName(chatBox.identifier);
            } else {
                return UserViewModel.getUserTitle(chatBox.identifier);
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
