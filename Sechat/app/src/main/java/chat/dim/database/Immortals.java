package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.format.Base64;
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

public class Immortals implements EntityDataSource, UserDataSource, GroupDataSource {

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

    @SuppressWarnings("unchecked")
    private Profile getProfile(Map dictionary, ID identifier, PrivateKey privateKey) {
        Profile profile;
        String profile_data = (String) dictionary.get("data");
        if (profile_data == null) {
            profile = new Profile(identifier);
            // set name
            String name = (String) dictionary.get("name");
            if (name == null) {
                List<String> names = (List<String>) dictionary.get("names");
                if (names != null) {
                    if (names.size() > 0) {
                        name = names.get(0);
                    }
                }
            }
            profile.setName(name);
            for (Object key : dictionary.keySet()) {
                if (key.equals("ID")) {
                    continue;
                }
                if (key.equals("name") || key.equals("names")) {
                    continue;
                }
                profile.setData((String) key, dictionary.get(key));
            }
            // sign profile
            profile.sign(privateKey);
        } else {
            String signature = (String) dictionary.get("signature");
            if (signature == null) {
                profile = new Profile(identifier, profile_data, null);
                // sign profile
                profile.sign(privateKey);
            } else {
                profile = new Profile(identifier, profile_data, Base64.decode(signature));
                // verify
                profile.verify(privateKey.getPublicKey());
            }
        }
        return profile;
    }

    private void loadBuiltInAccount(String filename) throws IOException, ClassNotFoundException {
        String jsonString = Resource.readTextFile(filename);
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
        // private key
        PrivateKey privateKey = PrivateKeyImpl.getInstance(dictionary.get("privateKey"));
        assert privateKey != null;
        if (meta.key.matches(privateKey)) {
            // TODO: store private key into keychain
            privateKeyMap.put(identifier.address, privateKey);
        } else {
            throw new IllegalArgumentException("private key not match meta public key:" + privateKey);
        }
        // profile
        Map profile = (Map) dictionary.get("profile");
        if (profile != null) {
            profileMap.put(identifier.address, getProfile(profile, identifier, privateKey));
        }
        // create user
        User user = new User(identifier);
        user.dataSource = this;
        userMap.put(identifier.address, user);
    }

    public ID getID(Object identifier) {
        return ID.getInstance(identifier);
    }

    public Account getAccount(ID identifier) {
        return userMap.get(identifier.address);
    }

    public User getUser(ID identifier) {
        return userMap.get(identifier.address);
    }

    public Group getGroup(ID identifier) {
        return null;
    }

    //---- EntityDataSource

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        // NOTICE: no need to save immortals meta
        return true;
    }

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
        PrivateKey privateKey = privateKeyMap.get(user.address);
        List<PrivateKey> list = new ArrayList<>();
        list.add(privateKey);
        return list;
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
}
