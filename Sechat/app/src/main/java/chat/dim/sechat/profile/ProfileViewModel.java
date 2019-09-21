package chat.dim.sechat.profile;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModel;

import java.util.List;

import chat.dim.client.Facebook;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.sechat.Client;

public class ProfileViewModel extends ViewModel {

    @SuppressLint("DefaultLocale")
    public String getNumberString(ID identifier) {
        long number = identifier.getNumber();
        String string = String.format("%010d", number);
        string = string.substring(0, 3) + "-" + string.substring(3, 6) + "-" + string.substring(6);
        return string;
    }

    public boolean existsContact(ID contact) {
        Client client = Client.getInstance();
        LocalUser user = client.getCurrentUser();
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
