package chat.dim.sechat.group;

import android.graphics.Bitmap;

import java.util.List;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.SharedGroupManager;
import chat.dim.mkm.User;
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

        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        User user = facebook.getCurrentUser();
        if (user == null) {
            return;
        }
        List<ID> contacts = user.getContacts();
        if (contacts != null) {
            SharedGroupManager manager = SharedGroupManager.getInstance();
            List<ID> allMembers = manager.getMembers(group);
            for (ID member : contacts) {
                if (!member.isUser()) {
                    continue;
                }
                if (allMembers.contains(member)) {
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
            GlobalVariable shared = GlobalVariable.getInstance();
            SharedFacebook facebook = shared.facebook;
            return facebook.getName(identifier);
        }
    }
}
