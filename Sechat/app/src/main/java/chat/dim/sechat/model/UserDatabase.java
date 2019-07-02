package chat.dim.sechat.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.common.Facebook;
import chat.dim.core.BarrackDelegate;
import chat.dim.crypto.PrivateKey;
import chat.dim.database.Immortals;
import chat.dim.database.MetaTable;
import chat.dim.database.ProfileTable;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.Profile;
import chat.dim.mkm.User;
import chat.dim.mkm.UserDataSource;
import chat.dim.mkm.entity.EntityDataSource;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class UserDatabase implements EntityDataSource, UserDataSource, GroupDataSource, BarrackDelegate {

    private static UserDatabase ourInstance = new UserDatabase();

    public static UserDatabase getInstance() {
        return ourInstance;
    }

    private UserDatabase() {

        // delegates
        Facebook barrack = Facebook.getInstance();
        barrack.entityDataSource = this;
        barrack.userDataSource   = this;
        barrack.groupDataSource  = this;
        barrack.delegate         = this;
    }

    // immortal accounts
    private Immortals immortals = Immortals.getInstance();

    // contacts list of each user
    private List<ID> contactList = new ArrayList<>();

    // profile cache
    private Map<Address, Profile> profileTable = new HashMap<>();

    private boolean cacheProfile(Profile profile) {
        Address key = profile == null ? null : profile.identifier.address;
        if (key == null) {
            return false;
        }
        profileTable.put(key, profile);
        return true;
    }

    //---- EntityDataSource

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        // TODO: save meta for ID
        return false;
    }

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = immortals.getMeta(identifier);
        if (meta != null) {
            return meta;
        }
        // load meta from database
        meta = MetaTable.loadMeta(identifier);
        if (meta == null) {
            // TODO: query from DIM network
        }
        return meta;
    }

    @Override
    public Profile getProfile(ID identifier) {
        // try from profile cache
        Profile profile = profileTable.get(identifier.address);
        if (profile != null) {
            // check cache expires
            Date now = new Date();
            Object timestamp = profile.get("lastTime");
            if (timestamp == null) {
                profile.put("lastTime", now.getTime() / 1000);
            } else {
                Date lastTime = new Date((long) timestamp * 1000);
                long dt = now.getTime() - lastTime.getTime();
                if (Math.abs(dt / 1000) > 3600) {
                    // profile expired
                    profileTable.remove(identifier.address);
                }
            }
            return profile;
        }
        do {
            // TODO: send query profile for updating from network

            // try from "/sdcard/chat.dim.sechat/.mkm/{address}/profile.js"
            profile = ProfileTable.loadProfile(identifier);
            if (profile != null) {
                break;
            }

            // try immortals
            profile = immortals.getProfile(identifier);
            if (profile != null) {
                break;
            }

            // place an empty profile for cache
            profile = new Profile(identifier);
            break;
        } while (true);

        profile.remove("lastTime");
        cacheProfile(profile);
        return profile;
    }

    //---- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        return null;
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        return null;
    }

    @Override
    public List<ID> getContacts(ID user) {
        // TODO: get contacts of the user
        return contactList;
    }

    //---- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        // TODO: get group founder ID
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        // TODO: get group owner ID
        return null;
    }

    @Override
    public List<ID> getMembers(ID group) {
        // TODO: get group members
        return null;
    }

    //---- BarrackDelegate

    @Override
    public Account getAccount(ID identifier) {
        // create extended Account object
        return null;
    }

    @Override
    public User getUser(ID identifier) {
        // create extended User object
        return null;
    }

    @Override
    public Group getGroup(ID identifier) {
        // create extended Group object
        return null;
    }

    static {
        // add test data
        UserDatabase facebook = UserDatabase.getInstance();
        facebook.contactList.add(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));
        facebook.contactList.add(ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj"));
    }
}
