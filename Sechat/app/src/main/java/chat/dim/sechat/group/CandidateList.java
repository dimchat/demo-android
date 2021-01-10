package chat.dim.sechat.group;

import android.graphics.Bitmap;

import java.util.List;

import chat.dim.User;
import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.protocol.ID;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

public class CandidateList extends DummyList<CandidateList.Item> {

    private final ID group;

    public CandidateList(ID identifier) {
        super();
        group = identifier;
    }

    @Override
    public synchronized void reloadData() {
        clearItems();

        Facebook facebook = Messenger.getInstance().getFacebook();
        User user = facebook.getCurrentUser();
        if (user == null) {
            return;
        }
        List<ID> contacts = user.getContacts();
        if (contacts != null) {
            for (ID member : contacts) {
                if (!member.isUser()) {
                    continue;
                }
                if (facebook.containsMember(member, group)) {
                    // already exists
                    continue;
                }
                addItem(new Item(member));
            }
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item implements DummyItem {

        private final ID identifier;

        Item(ID id) {
            super();
            identifier = id;
        }

        public ID getIdentifier() {
            return identifier;
        }

        Bitmap getAvatar() {
            return UserViewModel.getAvatar(identifier);
        }

        String getTitle() {
            Facebook facebook = Messenger.getInstance().getFacebook();
            return facebook.getName(identifier);
        }
    }
}
