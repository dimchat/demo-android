package chat.dim.sechat.search;

import android.graphics.Bitmap;

import java.util.List;

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.protocol.ID;
import chat.dim.protocol.SearchCommand;
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
        List<ID> users = response.getUsers();
        if (users == null || users.size() == 0) {
            return;
        }

        for (ID identifier : users) {
            addItem(new Item(identifier));
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    static class Item implements DummyItem {

        private final ID identifier;

        Item(ID id) {
            super();
            identifier = id;
        }

        ID getIdentifier() {
            return identifier;
        }

        Bitmap getAvatar() {
            return UserViewModel.getAvatar(getIdentifier());
        }

        String getTitle() {
            Facebook facebook = Messenger.getInstance().getFacebook();
            return facebook.getName(getIdentifier());
        }

        String getDesc() {
            return getIdentifier().getAddress().toString();
        }
    }
}
