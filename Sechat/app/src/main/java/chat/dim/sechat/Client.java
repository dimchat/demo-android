package chat.dim.sechat;

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
}
