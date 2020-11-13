package chat.dim.sechat.search;

import android.graphics.Bitmap;

import java.util.List;

import chat.dim.Entity;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.protocol.ID;
import chat.dim.protocol.SearchCommand;
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
public class DummyContent extends DummyList<DummyContent.Item> {

    private static Facebook facebook = Facebook.getInstance();

    public SearchCommand response = null;

    public void clearData() {
        response = null;
        clearItems();
    }

    @Override
    public synchronized void reloadData() {
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
            identifier = Entity.parseID(item);
            if (identifier == null) {
                continue;
            }
            addItem(new Item(identifier));
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    static class Item implements DummyItem {

        private final User account;

        Item(Object id) {
            super();
            account = facebook.getUser(Entity.parseID(id));
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
