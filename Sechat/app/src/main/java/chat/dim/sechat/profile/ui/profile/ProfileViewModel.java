package chat.dim.sechat.profile.ui.profile;

import android.arch.lifecycle.ViewModel;
import android.net.Uri;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.sechat.Client;

public class ProfileViewModel extends ViewModel {

    private static Facebook facebook = Facebook.getInstance();
    private static Client client = Client.getInstance();

    boolean existsContact(ID contact) {
        User user = client.getCurrentUser();
        if (user == null) {
            return false;
        }
        List<ID> contacts = facebook.getContacts(user.identifier);
        if (contacts == null) {
            return false;
        }
        return contacts.contains(contact);
    }

    Uri getAvatarUrl(ID contact) {
        String avatar = facebook.getAvatar(contact);
        if (avatar == null) {
            return null;
        }
        return Uri.parse(avatar);
    }
}
