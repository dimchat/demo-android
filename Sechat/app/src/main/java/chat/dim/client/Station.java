package chat.dim.client;

import java.util.Map;

import chat.dim.core.CompletionHandler;
import chat.dim.core.TransceiverDelegate;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.Account;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class Station extends Account implements TransceiverDelegate {

    public final String host;
    public final int port;

    public User currentUser;

    public Station(Map<String, Object> dictionary) {
        // ID
        super(ID.getInstance(dictionary.get("ID")));
        // host
        host = (String) dictionary.get("host");
        // port
        port = (int) dictionary.get("port");

        // SP
        // CA
    }

    //---- TransceiverDelegate

    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        return false;
    }

    @Override
    public SymmetricKey reuseCipherKey(ID sender, ID receiver, SymmetricKey reusedKey) {
        return null;
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
