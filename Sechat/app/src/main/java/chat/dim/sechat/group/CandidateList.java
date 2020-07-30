package chat.dim.sechat.group;

import android.net.Uri;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

public class CandidateList extends DummyList<CandidateList.Item> {

    private static Facebook facebook = Facebook.getInstance();

    private final ID group;

    public CandidateList(ID identifier) {
        super();
        group = identifier;
    }

    public void reloadData() {
        clearItems();

        User user = facebook.getCurrentUser();
        if (user != null) {
            List<ID> contacts = facebook.getContacts(user.identifier);
            if (contacts != null) {
                for (ID member : contacts) {
                    if (facebook.existsMember(member, group)) {
                        // already exists
                        continue;
                    }
                    addItem(new Item(member));
                }
            }
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item extends DummyItem {

        private final User account;

        Item(Object id) {
            super();
            account = UserViewModel.getUser(id);
        }

        ID getIdentifier() {
            return account.identifier;
        }

        Uri getAvatarUri() {
            return UserViewModel.getAvatarUri(account.identifier);
        }

        String getTitle() {
            return UserViewModel.getUserTitle(account.identifier);
        }

        String getDesc() {
            return EntityViewModel.getAddressString(account.identifier);
        }
    }
}
