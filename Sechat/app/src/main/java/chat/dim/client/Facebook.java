package chat.dim.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import chat.dim.core.Barrack;
import chat.dim.core.BarrackDelegate;
import chat.dim.crypto.PublicKey;
import chat.dim.database.MetaTable;
import chat.dim.format.JSON;
import chat.dim.group.Chatroom;
import chat.dim.group.Polylogue;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.mkm.entity.NetworkType;
import chat.dim.mkm.entity.Profile;

public class Facebook extends Barrack implements BarrackDelegate {

    private static final Facebook ourInstance = new Facebook();

    public static Facebook getInstance() {
        return ourInstance;
    }

    private Facebook() {
    }

    public boolean verifyProfile(Profile profile) {
        if (profile.isValid()) {
            // already verified
            return true;
        }
        ID identifier = profile.identifier;
        NetworkType type = identifier.getType();
        PublicKey key = null;
        if (type.isCommunicator()) {
            // verify with meta.key
            Meta meta = getMeta(identifier);
            key = meta == null ? null : meta.key;
        } else if (type.isGroup()) {
            // verify with group owner's meta.key
            Group group = getGroup(identifier);
            Meta meta = getMeta(group.getOwner());
            key = meta == null ? null : meta.key;
        }
        return key != null && profile.verify(key);
    }

    //-------- EntityDataSource

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        if (super.saveMeta(meta, identifier)) {
            return true;
        }
        return MetaTable.saveMeta(meta, identifier);
    }

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = super.getMeta(identifier);
        if (meta != null) {
            return meta;
        }
        // load from local storage
        meta = MetaTable.loadMeta(identifier);
        if (meta != null && cacheMeta(meta, identifier)) {
            return meta;
        }
        assert meta == null;
        return null;
    }

    @Override
    public Profile getProfile(ID identifier) {
        Profile profile = super.getProfile(identifier);
        if (profile == null || profile.isValid()) {
            // already verified?
            return profile;
        }
        if (verifyProfile(profile)) {
            // signature correct
            return profile;
        }
        // profile error?
        return profile;
    }

    //-------- DIMBarrackDelegate

    @Override
    public Account getAccount(ID identifier) {
        Account account = super.getAccount(identifier);
        if (account != null) {
            return account;
        }
        // check meta
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        // create it with type
        NetworkType type = identifier.getType();
        if (type.isStation()) {
            account = new Server(identifier);
        } else if (type.isPerson()) {
            account = new Account(identifier);
        }
        assert account != null;
        cacheAccount(account);
        return account;
    }

    @Override
    public User getUser(ID identifier) {
        User user = super.getUser(identifier);
        if (user != null) {
            return user;
        }
        // check meta
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        // TODO: check private key

        // create it
        user = new User(identifier);
        cacheUser(user);
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        Group group = super.getGroup(identifier);
        if (group != null) {
            return group;
        }
        // check meta
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        // create it with type
        NetworkType type = identifier.getType();
        if (type == NetworkType.Polylogue) {
            group = new Polylogue(identifier);
        } else if (type == NetworkType.Chatroom) {
            group = new Chatroom(identifier);
        }
        assert group != null;
        cacheGroup(group);
        return group;
    }
}
