/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.common;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.crypto.impl.PublicKeyImpl;
import chat.dim.database.AddressNameTable;
import chat.dim.database.ContactTable;
import chat.dim.database.GroupTable;
import chat.dim.database.Immortals;
import chat.dim.database.MetaTable;
import chat.dim.database.PrivateTable;
import chat.dim.database.ProfileTable;
import chat.dim.database.UserTable;
import chat.dim.extension.BTCMeta;
import chat.dim.extension.ECCPrivateKey;
import chat.dim.extension.ECCPublicKey;
import chat.dim.extension.ETHMeta;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.mkm.User;

public class Facebook extends chat.dim.Facebook {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    private Immortals immortals = Immortals.getInstance();

    private PrivateTable privateTable = new PrivateTable();
    private MetaTable metaTable = new MetaTable();
    private ProfileTable profileTable = new ProfileTable();

    private AddressNameTable ansTable = new AddressNameTable();

    private UserTable userTable = new UserTable();
    private GroupTable groupTable = new GroupTable();
    private ContactTable contactTable = new ContactTable();

    //-------- Address Name Service

    public boolean saveAnsRecord(String name, ID identifier) {
        return ansTable.saveRecord(name, identifier);
    }

    public ID ansRecord(String name) {
        return ansTable.record(name);
    }

    public Set<String> ansNames(String identifier) {
        return ansTable.names(identifier);
    }

    //-------- User

    public User getCurrentUser() {
        return getUser(userTable.getCurrentUser());
    }

    public void setCurrentUser(User user) {
        userTable.setCurrentUser(user.identifier);
    }

    public List<ID> allUsers() {
        return userTable.allUsers();
    }

    public boolean addUser(ID user) {
        return userTable.addUser(user);
    }

    public boolean removeUser(ID user) {
        return userTable.removeUser(user);
    }

    public boolean addContact(ID contact, ID user) {
        return contactTable.addContact(contact, user);
    }

    public boolean removeContact(ID contact, ID user) {
        return contactTable.removeContact(contact, user);
    }

    //---- Private Key

    @Override
    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        return privateTable.savePrivateKey(privateKey, identifier);
    }

    @Override
    protected PrivateKey loadPrivateKey(ID user) {
        // FIXME: which key?
        PrivateKey key = privateTable.getPrivateKeyForSignature(user);
        if (key == null) {
            // try immortals
            key = (PrivateKey) immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    //---- Meta

    @Override
    public boolean saveMeta(Meta meta, ID entity) {
        if (!verify(meta, entity)) {
            // meta not match ID
            return false;
        }
        return metaTable.saveMeta(meta, entity);
    }

    @Override
    protected Meta loadMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            // broadcast ID has not meta
            return null;
        }
        Meta meta = metaTable.getMeta(identifier);
        if (meta == null) {
            // try immortals
            meta = immortals.getMeta(identifier);
            if (meta == null) {
                // TODO: query from DIM network
            }
        }
        return meta;
    }

    //---- Profile

    @Override
    public boolean saveProfile(Profile profile) {
        if (!verify(profile)) {
            // profile's signature not match
            return false;
        }
        return profileTable.saveProfile(profile);
    }

    @Override
    protected Profile loadProfile(ID identifier) {
        Profile profile = profileTable.getProfile(identifier);
        if (profile == null) {
            // try immortals
            profile = immortals.getProfile(identifier);
            if (profile == null) {
                // TODO: query from DIM network
            }
        }
        return profile;
    }

    //---- Relationship

    @Override
    public boolean saveContacts(List<ID> contacts, ID user) {
        return contactTable.saveContacts(contacts, user);
    }

    @Override
    protected List<ID> loadContacts(ID user) {
        List<ID> contacts = contactTable.getContacts(user);
        if (contacts == null || contacts.size() == 0) {
            // try immortals
            contacts = immortals.getContacts(user);
        }
        return contacts;
    }

    public boolean addMember(ID member, ID group) {
        return groupTable.addMember(member, group);
    }

    public boolean removeMember(ID member, ID group) {
        return groupTable.removeMember(member, group);
    }

    @Override
    public boolean saveMembers(List<ID> members, ID group) {
        return groupTable.saveMembers(members, group);
    }

    @Override
    protected List<ID> loadMembers(ID group) {
        return groupTable.getMembers(group);
    }

    //--------

    public String getNickname(ID identifier) {
        assert identifier.getType().isUser();
        User user = getUser(identifier);
        return user == null ? null : user.getName();
    }

    public String getNumberString(ID identifier) {
        long number = identifier.getNumber();
        String string = String.format(Locale.CHINA, "%010d", number);
        string = string.substring(0, 3) + "-" + string.substring(3, 6) + "-" + string.substring(6);
        return string;
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        // get from database
        ID founder = groupTable.getFounder(group);
        if (founder != null) {
            return founder;
        }
        return super.getFounder(group);
    }

    @Override
    public ID getOwner(ID group) {
        // get from database
        ID owner = groupTable.getOwner(group);
        if (owner != null) {
            return owner;
        }
        return super.getOwner(group);
    }

    static {
        // register new asymmetric cryptography key classes
        PrivateKeyImpl.register(PrivateKey.ECC, ECCPrivateKey.class);
        PublicKeyImpl.register(PublicKey.ECC, ECCPublicKey.class);

        // register new address classes
//        Address.register(BTCAddress.class);
//        Address.register(ETHAddress.class);

        // register new meta classes
        Meta.register(Meta.VersionBTC, BTCMeta.class);
        Meta.register(Meta.VersionExBTC, BTCMeta.class);
        Meta.register(Meta.VersionETH, ETHMeta.class);
        Meta.register(Meta.VersionExETH, ETHMeta.class);
    }
}
