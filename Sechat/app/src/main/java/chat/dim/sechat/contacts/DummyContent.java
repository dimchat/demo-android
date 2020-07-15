package chat.dim.sechat.contacts;

import android.net.Uri;

import java.util.List;
import java.util.Locale;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
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
            account = facebook.getUser(ID.getInstance(id));
        }

        ID getIdentifier() {
            return account.identifier;
        }

        Uri getAvatarUrl() {
            ID identifier = getIdentifier();
            if (identifier == null) {
                return null;
            }
            String avatar = facebook.getAvatar(identifier);
            if (avatar == null) {
                return null;
            }
            return Uri.parse(avatar);
        }

        String getTitle() {
            String nickname = account.getName();
            String number = facebook.getNumberString(account.identifier);
            return String.format(Locale.CHINA, "%s (%s)", nickname, number);
        }

        String getDesc() {
            return account.identifier.toString();
        }
    }
}
