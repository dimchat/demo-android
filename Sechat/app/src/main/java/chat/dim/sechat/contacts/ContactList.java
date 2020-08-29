package chat.dim.sechat.contacts;

import android.graphics.Bitmap;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.protocol.LoginCommand;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;
import chat.dim.utils.Times;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class ContactList extends DummyList<ContactList.Item> {

    private static Collator collator = Collator.getInstance(Locale.CHINA);

    @Override
    public synchronized void reloadData() {
        clearItems();

        User user = UserViewModel.getCurrentUser();
        if (user != null) {
            List<ID> contacts = UserViewModel.getContacts(user.identifier);
            if (contacts != null) {
                // sort by nickname
                Comparator<ID> comparator = (uid1, uid2) -> {
                    String name1 = EntityViewModel.getName(uid1);
                    if (name1 == null) {
                        name1 = "";
                    }
                    String name2 = EntityViewModel.getName(uid2);
                    if (name2 == null) {
                        name2 = "";
                    }
                    return collator.compare(name1, name2);
                };
                Collections.sort(contacts, comparator);
                for (ID identifier : contacts) {
                    if (identifier.isGroup()) {
                        // FIXME: where to show groups?
                        continue;
                    }
                    addItem(new Item(identifier));
                }
            }
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item implements DummyItem {

        private final ID identifier;

        Item(ID id) {
            super();
            identifier = id;
        }

        ID getIdentifier() {
            return identifier;
        }

        Bitmap getAvatar() {
            if (identifier.isGroup()) {
                return GroupViewModel.getLogo(identifier);
            }
            return UserViewModel.getAvatar(identifier);
        }

        String getTitle() {
            if (identifier.isGroup()) {
                // TODO: show group title with format "group name (members count)"
                return EntityViewModel.getName(identifier);
            }
            return UserViewModel.getUserTitle(identifier);
        }

        String getDesc() {
            if (identifier.isGroup()) {
                return null;
            }
            LoginCommand cmd = UserViewModel.getLoginCommand(identifier);
            if (cmd != null) {
                Map<String, Object> info = cmd.getStation();
                ID sid = EntityViewModel.getID(info.get("ID"));
                if (cmd.time != null) {
                    if (sid != null) {
                        return "Last login [" + Times.getTimeString(cmd.time) + "]: " + EntityViewModel.getName(sid);
                    } else {
                        return "Last login [" + Times.getTimeString(cmd.time) + "]";
                    }
                } else if (sid != null) {
                    return "Last login station: " + EntityViewModel.getName(sid);
                }
            }
            //return EntityViewModel.getAddressString(account.identifier);
            return null;
        }
    }
}
