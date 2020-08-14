package chat.dim.sechat.history;

import android.graphics.Bitmap;

import java.util.ArrayList;
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
import chat.dim.utils.Times;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class ConversationList extends DummyList<ConversationList.Item> {

    private static Amanuensis clerk = Amanuensis.getInstance();
    private static ConversationDatabase msgDB = ConversationDatabase.getInstance();

    @Override
    public synchronized void reloadData() {

        List<Conversation> conversations = new ArrayList<>();
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
            conversations.add(chatBox);
        }
        // refresh items
        clearItems();
        for (int index = 0; index < count; index++) {
            chatBox = conversations.get(index);
            addItem(new Item(chatBox));
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    static class Item implements DummyItem {

        private final Conversation chatBox;

        Item(Conversation chatBox) {
            super();
            this.chatBox = chatBox;
        }

        ID getIdentifier() {
            return chatBox.identifier;
        }

        Bitmap getLogo() {
            return GroupViewModel.getLogo(chatBox.identifier);
        }

        Bitmap getAvatar() {
            return UserViewModel.getAvatar(chatBox.identifier);
        }

        String getTitle() {
            return EntityViewModel.getName(chatBox.identifier);
        }

        String getTime() {
            String time = "";
            InstantMessage iMsg = chatBox.getLastVisibleMessage();
            if (iMsg != null && iMsg.envelope.getTime() != null) {
                time = Times.getTimeString(iMsg.envelope.getTime());
            }
            return time;
        }

        String getDesc() {
            String text = "(no message)";
            InstantMessage iMsg = chatBox.getLastVisibleMessage();
            if (iMsg != null) {
                text = msgDB.getContentText(iMsg.getContent());
            }
            return text;
        }

        String getUnread() {
            int count = msgDB.numberOfUnreadMessages(chatBox);
            if (count > 0) {
                return "" + count;
            }
            return null;
        }
    }
}
