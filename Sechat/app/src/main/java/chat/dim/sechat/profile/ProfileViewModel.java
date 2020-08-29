package chat.dim.sechat.profile;

import android.net.Uri;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.sechat.model.UserViewModel;

public class ProfileViewModel extends UserViewModel {

    Uri getAvatarUri() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("ID not set");
        }
        String avatar = facebook.getAvatar(identifier);
        if (avatar == null) {
            return null;
        }
        return Uri.parse(avatar);
    }

    boolean existsContact(ID contact) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        List<ID> contacts = facebook.getContacts(user.identifier);
        if (contacts == null) {
            return false;
        }
        return contacts.contains(contact);
    }
}
