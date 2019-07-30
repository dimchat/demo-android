package chat.dim.database;

import java.util.Map;

import chat.dim.core.KeyCache;
import chat.dim.crypto.SymmetricKey;
import chat.dim.mkm.entity.ID;

public class KeyStore extends KeyCache {

    public KeyStore() {
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
