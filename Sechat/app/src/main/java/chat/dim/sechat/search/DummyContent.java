package chat.dim.sechat.search;

import android.net.Uri;

import java.util.List;
import java.util.Locale;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.protocol.SearchCommand;
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

    public SearchCommand response = null;

    DummyContent() {
        super();
        reloadData();
    }

    public void reloadData() {
        clearItems();

        if (response == null) {
            return;
        }
        List<String> users = response.getUsers();
        if (users == null || users.size() == 0) {
            return;
        }

        ID identifier;
        for (String item : users) {
            identifier = facebook.getID(item);
            if (identifier == null) {
                continue;
            }
            addItem(new Item(identifier));
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
