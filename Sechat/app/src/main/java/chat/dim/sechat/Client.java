package chat.dim.sechat;

import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.network.Terminal;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;

public class Client extends Terminal {

    private static final Client ourInstance = new Client();
    public static Client getInstance() { return ourInstance; }
    private Client() {
        super();
    }

    public String getDisplayName() {
        return "DIM!";
    }

    public void startChat(ID entity) {
        Map<String, Object> info = new HashMap<>();
        info.put("ID", entity);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.StartChat, this, info);
    }
}
