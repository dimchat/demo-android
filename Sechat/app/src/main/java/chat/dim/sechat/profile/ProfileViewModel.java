package chat.dim.sechat.profile;

import android.net.Uri;

import java.util.List;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.common.BackgroundThread;
import chat.dim.model.Messenger;
import chat.dim.sechat.model.UserViewModel;

public class ProfileViewModel extends UserViewModel {

    public static void updateProfile(ID identifier) {
        if (identifier == null) {
            return;
        }
        BackgroundThread.wait(() -> {
            Messenger messenger = Messenger.getInstance();
            Profile profile = facebook.getProfile(identifier);
            if (profile == null || facebook.isExpired(profile)) {
                messenger.queryProfile(identifier);
            }
        });
    }

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
