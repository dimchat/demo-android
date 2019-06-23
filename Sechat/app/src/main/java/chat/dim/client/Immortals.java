package chat.dim.client;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.core.BarrackDelegate;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.format.JSON;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.entity.Profile;
import chat.dim.mkm.User;
import chat.dim.mkm.UserDataSource;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.EntityDataSource;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class Immortals implements EntityDataSource, UserDataSource, GroupDataSource, BarrackDelegate {

    private static final Immortals ourInstance = new Immortals();

    public static Immortals getInstance() {
        return ourInstance;
    }

    private Immortals() {
        try {
            loadBuiltInAccount("/mkm_hulk.js");
            loadBuiltInAccount("/mkm_moki.js");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private final Map<Address, Meta>    metaMap       = new HashMap<>();
    private final Map<Address, User>    userMap       = new HashMap<>();
    private Map<Address, PrivateKey>    privateKeyMap = new HashMap<>();
    private final Map<Address, Profile> profileMap    = new HashMap<>();

    private void loadBuiltInAccount(String filename) throws IOException, ClassNotFoundException {
        String jsonString = FileUtils.readTextFile(filename);
        Map<String, Object> dictionary = JSON.decode(jsonString);
        System.out.println(filename + ":" + dictionary);
        // ID
        ID identifier = ID.getInstance(dictionary.get("ID"));
        assert identifier != null;
        // meta
        Meta meta = Meta.getInstance(dictionary.get("meta"));
        assert meta != null;
        if (meta.matches(identifier)) {
            metaMap.put(identifier.address, meta);
        } else {
            throw new IllegalArgumentException("meta not match ID:" + identifier + ", " + meta);
        }
        // profile
        Object profile = dictionary.get("profile");
        if (profile != null) {
            profileMap.put(identifier.address, Profile.getInstance(profile));
        }
        // private key
        PrivateKey privateKey = PrivateKeyImpl.getInstance(dictionary.get("privateKey"));
        if (meta.key.matches(privateKey)) {
            // TODO: store private key into keychain
            privateKeyMap.put(identifier.address, privateKey);
        } else {
            throw new IllegalArgumentException("private key not match meta public key:" + privateKey);
        }
        // create user
        User user = new User(identifier);
        user.dataSource = this;
        userMap.put(identifier.address, user);
    }

    //---- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        return metaMap.get(identifier.address);
    }

    @Override
    public Profile getProfile(ID identifier) {
        return profileMap.get(identifier.address);
    }

    //---- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        return privateKeyMap.get(user.address);
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        return null;
    }

    @Override
    public List<ID> getContacts(ID user) {
        return null;
    }

    //---- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        return null;
    }

    //---- BarrackDelegate

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        // NOTICE: no need to save immortals meta
        return true;
    }

    @Override
    public Account getAccount(ID identifier) {
        return userMap.get(identifier.address);
    }

    @Override
    public User getUser(ID identifier) {
        return userMap.get(identifier.address);
    }

    @Override
    public Group getGroup(ID identifier) {
        return null;
    }
}
