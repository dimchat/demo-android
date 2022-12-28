package chat.dim.sechat.contacts;

import android.graphics.Bitmap;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.mkm.User;
import chat.dim.protocol.ID;
import chat.dim.protocol.LoginCommand;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.type.Time;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

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

        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        User user = facebook.getCurrentUser();
        if (user != null) {
            List<ID> contacts = user.getContacts();
            if (contacts != null) {
                // sort by nickname
                Comparator<ID> comparator = (uid1, uid2) -> {
                    String name1 = facebook.getName(uid1);
                    if (name1 == null) {
                        name1 = "";
                    }
                    String name2 = facebook.getName(uid2);
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
            GlobalVariable shared = GlobalVariable.getInstance();
            SharedFacebook facebook = shared.facebook;
            // TODO: show group title with format "group name (members count)"
            return facebook.getName(identifier);
        }

        String getDesc() {
            if (identifier.isGroup()) {
                return null;
            }
            LoginCommand cmd = UserViewModel.getLoginCommand(identifier);
            if (cmd != null) {
                GlobalVariable shared = GlobalVariable.getInstance();
                SharedFacebook facebook = shared.facebook;
                Map<String, Object> info = cmd.getStation();
                ID sid = ID.parse(info.get("ID"));
                Date time = cmd.getTime();
                if (time != null) {
                    if (sid != null) {
                        return "Last login [" + Time.getTimeString(time) + "]: " + facebook.getName(sid);
                    } else {
                        return "Last login [" + Time.getTimeString(time) + "]";
                    }
                } else if (sid != null) {
                    return "Last login station: " + facebook.getName(sid);
                }
            }
            //return EntityViewModel.getAddressString(identifier);
            return null;
        }
    }
}
