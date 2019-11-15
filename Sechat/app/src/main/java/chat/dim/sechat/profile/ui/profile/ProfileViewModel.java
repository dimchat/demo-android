package chat.dim.sechat.profile.ui.profile;

import android.arch.lifecycle.ViewModel;

import java.util.List;

import chat.dim.common.Facebook;
import chat.dim.mkm.ID;
import chat.dim.mkm.User;
import chat.dim.sechat.Client;

class ProfileViewModel extends ViewModel {

    boolean existsContact(ID contact) {
        Client client = Client.getInstance();
        User user = client.getCurrentUser();
        if (user == null) {
            return false;
        }
        Facebook facebook = Facebook.getInstance();
        List<ID> contacts = facebook.getContacts(user.identifier);
        if (contacts == null) {
            return false;
        }
        return contacts.contains(contact);
    }
}
