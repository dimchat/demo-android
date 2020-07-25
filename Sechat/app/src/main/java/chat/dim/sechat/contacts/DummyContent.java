package chat.dim.sechat.contacts;

import android.net.Uri;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
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

    private static Facebook facebook = Facebook.getInstance();

    DummyContent() {
        super();
        reloadData();
    }

    public void reloadData() {
        clearItems();

        User user = facebook.getCurrentUser();
        if (user != null) {
            List<ID> contacts = facebook.getContacts(user.identifier);
            if (contacts != null) {
                for (ID identifier : contacts) {
                    addItem(new Item(identifier));
                }
            }
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    class Item extends DummyItem {

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
            return account.identifier.toString();
        }
    }
}
