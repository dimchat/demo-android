package chat.dim.sechat.search;

import android.graphics.Bitmap;

import java.util.List;

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
            identifier = ID.parse(item);
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

        private final ID identifier;

        Item(Object id) {
            super();
            identifier = ID.parse(id);
        }

        ID getIdentifier() {
            return identifier;
        }

        Bitmap getAvatar() {
            return UserViewModel.getAvatar(getIdentifier());
        }

        String getTitle() {
            return UserViewModel.getUserTitle(getIdentifier());
        }

        String getDesc() {
            return EntityViewModel.getAddressString(getIdentifier());
        }
    }
}
