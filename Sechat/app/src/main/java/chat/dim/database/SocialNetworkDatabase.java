package chat.dim.database;

import java.util.List;

import chat.dim.client.Facebook;
import chat.dim.client.FacebookDelegate;
import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.UserDataSource;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.mkm.entity.Profile;

public class SocialNetworkDatabase implements UserDataSource, GroupDataSource, FacebookDelegate {
    private static final SocialNetworkDatabase ourInstance = new SocialNetworkDatabase();
    public static SocialNetworkDatabase getInstance() { return ourInstance; }
    private SocialNetworkDatabase() {
        // initialized delegates of facebook
        Facebook facebook = Facebook.getInstance();
        facebook.entityDataSource = this;
        facebook.userDataSource = this;
        facebook.groupDataSource = this;
        facebook.delegate = this;
    }

    private static Immortals immortals = Immortals.getInstance();

    public void reloadData(ID user) {
        // reload contacts for current user
        UserTable.reloadData(user);
        // reload conversation database
        ConversationDatabase.getInstance().reloadData(user);
    }

    //---- EntityDataSource

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        return MetaTable.saveMeta(meta, identifier);
    }

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = MetaTable.getMeta(identifier);
        if (meta == null && identifier.getType().isPerson()) {
            meta = immortals.getMeta(identifier);
        }
        return meta;
    }

    @Override
    public Profile getProfile(ID identifier) {
        Profile profile = ProfileTable.getProfile(identifier);
        if (profile == null && identifier.getType().isPerson()) {
            profile = immortals.getProfile(identifier);
        }
        return profile;
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = UserTable.getPrivateKeyForSignature(user);
        if (key == null && user.getType().isPerson()) {
            key = immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        List<PrivateKey> keys = UserTable.getPrivateKeysForDecryption(user);
        if (keys == null && user.getType().isPerson()) {
            keys = immortals.getPrivateKeysForDecryption(user);
        }
        return keys;
    }

    @Override
    public List<ID> getContacts(ID user) {
        return UserTable.getContacts(user);
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        return GroupTable.getFounder(group);
    }

    @Override
    public ID getOwner(ID group) {
        return GroupTable.getOwner(group);
    }

    @Override
    public List<ID> getMembers(ID group) {
        return GroupTable.getMembers(group);
    }

    //-------- FacebookDelegate

    @Override
    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        return UserTable.savePrivateKey(privateKey, identifier);
    }

    @Override
    public boolean saveProfile(Profile profile) {
        return ProfileTable.saveProfile(profile);
    }
}
