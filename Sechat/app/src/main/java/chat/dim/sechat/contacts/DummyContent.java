package chat.dim.sechat.contacts;

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

    DummyContent() {
        super();
        reloadData();
    }

    public void reloadData() {
        clearItems();

        Facebook facebook = Facebook.getInstance();
        User user = facebook.getCurrentUser();
        if (user != null) {
            List<ID> contacts = facebook.getContacts(user.identifier);
            for (ID identifier : contacts) {
                addItem(new Item(identifier));
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
            account = Facebook.getInstance().getUser(ID.getInstance(id));
        }

        ID getIdentifier() {
            return account.identifier;
        }

        String getTitle() {
            String nickname = account.getName();
            String number = Facebook.getInstance().getNumberString(account.identifier);
            return String.format(Locale.CHINA, "%s (%s)", nickname, number);
        }

        String getDesc() {
            return account.identifier.toString();
        }
    }
}
