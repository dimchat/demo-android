package chat.dim.sechat.group;

import android.graphics.Bitmap;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.GroupViewModel;
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

        User user = UserViewModel.getCurrentUser();
        if (user == null) {
            return;
        }
        List<ID> contacts = UserViewModel.getContacts(user.identifier);
        if (contacts != null) {
            for (ID member : contacts) {
                if (GroupViewModel.existsMember(member, group)) {
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

        private final User account;

        Item(Object id) {
            super();
            account = UserViewModel.getUser(id);
        }

        ID getIdentifier() {
            return account.identifier;
        }

        Bitmap getAvatar() {
            return UserViewModel.getAvatar(account.identifier);
        }

        String getTitle() {
            return UserViewModel.getUserTitle(account.identifier);
        }

        String getDesc() {
            return EntityViewModel.getAddressString(account.identifier);
        }
    }
}
