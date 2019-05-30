package chat.dim.sechat.model;

import java.util.ArrayList;
import java.util.List;

import chat.dim.client.Immortals;
import chat.dim.core.Barrack;
import chat.dim.core.BarrackDelegate;
import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.Profile;
import chat.dim.mkm.User;
import chat.dim.mkm.UserDataSource;
import chat.dim.mkm.entity.Entity;
import chat.dim.mkm.entity.EntityDataSource;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;

public class Facebook implements EntityDataSource, UserDataSource, GroupDataSource, BarrackDelegate {

    private static Facebook ourInstance = new Facebook();

    public static Facebook getInstance() {
        return ourInstance;
    }

    private Facebook() {

        // delegates
        Barrack barrack = Barrack.getInstance();
        barrack.entityDataSource = this;
        barrack.userDataSource   = this;
        barrack.groupDataSource  = this;
        barrack.delegate         = this;
    }

    Immortals immortals = Immortals.getInstance();

    List<ID> contactList = new ArrayList<>();

    //---- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        // TODO: load meta for ID
        return null;
    }

    //---- ProfileDataSource

    @Override
    public Profile getProfile(ID identifier) {
        // TODO: get profile for ID
        return null;
    }

    //---- UserDataSource

    @Override
    public PrivateKey getPrivateKey(int flag, ID user) {
        // TODO: load private key for user
        return null;
    }

    @Override
    public List<ID> getContacts(ID user) {
        // TODO: get contacts of the user
        return contactList;
    }

    @Override
    public int getCountOfContacts(ID user) {
        // TODO: get contacts count
        return contactList.size();
    }

    @Override
    public ID getContactAtIndex(int index, ID user) {
        // TODO: get one contact
        return ID.getInstance(contactList.get(index));
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

    @Override
    public int getCountOfMembers(ID group) {
        // TODO: get members count
        return 0;
    }

    @Override
    public ID getMemberAtIndex(int index, ID group) {
        // TODO: get one member
        return null;
    }

    //---- BarrackDelegate

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        // TODO: save meta for ID
        return false;
    }

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
        Facebook facebook = Facebook.getInstance();
        facebook.contactList.add(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));
        facebook.contactList.add(ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj"));
    }
}
