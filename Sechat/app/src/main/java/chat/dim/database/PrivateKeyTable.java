package chat.dim.database;

import java.util.List;

import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.entity.ID;

public class PrivateKeyTable extends Resource {

    public static PrivateKey getPrivateKeyForSignature(ID user) {
        // TODO: get private key for signature
        return null;
    }

    public static List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        // TODO: get private keys for decryption
        return null;
    }

    public static boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        // TODO: save private key for ID
        return false;
    }
}
