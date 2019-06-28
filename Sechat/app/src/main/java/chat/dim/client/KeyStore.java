package chat.dim.client;

import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.mkm.entity.ID;

public class KeyStore extends chat.dim.core.KeyStore {

    KeyStore() {
        super();
    }

    @Override
    public boolean saveKeys(Map keyMap) {
        return false;
    }

    @Override
    public Map loadKeys() {
        return null;
    }

    @Override
    public SymmetricKey reuseCipherKey(ID sender, ID receiver, SymmetricKey key) {
        return null;
    }
}
