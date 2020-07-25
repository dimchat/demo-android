package chat.dim.sechat.profile;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.sechat.Client;
import chat.dim.sechat.model.UserViewModel;

public class ProfileViewModel extends UserViewModel {

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
}
