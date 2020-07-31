package chat.dim.sechat.contacts;

import android.graphics.Bitmap;
import android.net.Uri;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class ContactList extends DummyList<ContactList.Item> {

    private static Facebook facebook = Facebook.getInstance();

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
    public static class Item extends DummyItem {

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
