package chat.dim.client;

import java.util.Map;

import chat.dim.core.CompletionHandler;
import chat.dim.core.TransceiverDelegate;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.network.Station;

public class Server extends Station implements TransceiverDelegate {

    public User currentUser;

    public Server(ID identifier) {
        super(identifier);
    }

    public Server(ID identifier, String host, int port) {
        super(identifier, host, port);
    }

    public Server(Map<String, Object> dictionary) {
        this(ID.getInstance(dictionary.get("ID")),
                (String) dictionary.get("host"),
                (int) dictionary.get("port"));

        // SP
        // CA
    }

    //---- TransceiverDelegate

    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        return false;
    }

    @Override
    public String uploadFileData(byte[] data, InstantMessage iMsg) {
        return null;
    }

    @Override
    public byte[] downloadFileData(String url, InstantMessage iMsg) {
        return new byte[0];
    }
}
