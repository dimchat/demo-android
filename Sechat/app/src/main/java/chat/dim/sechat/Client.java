package chat.dim.sechat;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.network.Terminal;

public class Client extends Terminal {

    private static final Client ourInstance = new Client();
    public static Client getInstance() { return ourInstance; }
    private Client() {
        super();
    }

    public String getDisplayName() {
        return "DIM!";
    }

    static {
        Facebook facebook = Facebook.getInstance();

        // FIXME: test data
        ID hulk = facebook.getID("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");
        ID moki = facebook.getID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");

        User user = facebook.getUser(hulk);
        facebook.setCurrentUser(user);
        facebook.addContact(moki, user.identifier);
    }
}
