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
package chat.dim.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import chat.dim.AddressNameService;
import chat.dim.ID;
import chat.dim.Immortals;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.PrivateKey;
import chat.dim.database.AddressNameTable;
import chat.dim.database.ContactTable;
import chat.dim.database.GroupTable;
import chat.dim.database.MetaTable;
import chat.dim.database.PrivateTable;
import chat.dim.database.ProfileTable;
import chat.dim.database.UserTable;
import chat.dim.protocol.NetworkType;

public class Facebook extends chat.dim.Facebook {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();

        // ANS
        ans = new AddressNameService() {
            @Override
            public ID identifier(String name) {
                return ansTable.record(name);
            }

            @Override
            public boolean save(String name, ID identifier) {
                return ansTable.saveRecord(name, identifier);
            }
        };
        setANS(ans);
    }

    private final AddressNameService ans;
    private Immortals immortals = new Immortals();

    private PrivateTable privateTable = new PrivateTable();
    private MetaTable metaTable = new MetaTable();
    private ProfileTable profileTable = new ProfileTable();

    private AddressNameTable ansTable = new AddressNameTable();

    private UserTable userTable = new UserTable();
    private GroupTable groupTable = new GroupTable();
    private ContactTable contactTable = new ContactTable();

    private List<User> users = null;

    //-------- Local Users

    @Override
    public List<User> getLocalUsers() {
        if (users == null) {
            users = new ArrayList<>();
            List<ID> list = userTable.allUsers();
            User user;
            for (ID item : list) {
                user = getUser(item);
                if (user == null) {
                    throw new NullPointerException("failed to get local user: " + item);
                }
                users.add(user);
            }
        }
        return users;
    }

    @Override
    public User getCurrentUser() {
        return getUser(userTable.getCurrentUser());
    }

    public void setCurrentUser(User user) {
        users = null;
        userTable.setCurrentUser(user.identifier);
    }

    public boolean addUser(ID user) {
        users = null;
        return userTable.addUser(user);
    }

    public boolean removeUser(ID user) {
        users = null;
        return userTable.removeUser(user);
    }

    //-------- Contacts

    public boolean addContact(ID contact, ID user) {
        return contactTable.addContact(contact, user);
    }

    public boolean removeContact(ID contact, ID user) {
        return contactTable.removeContact(contact, user);
    }

    //-------- Private Key

    @Override
    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        if (!verify(privateKey, identifier)) {
            // private key not match meta.key
            return false;
        }
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

    //-------- Meta

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
        // try from database
        Meta meta = metaTable.getMeta(identifier);
        if (meta != null) {
            return meta;
        }
        // try from immortals
        if (identifier.getType() == NetworkType.Main.value) {
            meta = immortals.getMeta(identifier);
            if (meta != null) {
                return meta;
            }
        }
        // query from DIM network
        Messenger messenger = Messenger.getInstance();
        messenger.queryMeta(identifier);
        return null;
    }

    //-------- Profile

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
        // try from database
        Profile profile = profileTable.getProfile(identifier);
        if (profile != null) {
            // is empty?
            Set<String> names = profile.propertyNames();
            if (names != null && names.size() > 0) {
                return profile;
            }
        }
        // try from immortals
        if (identifier.getType() == NetworkType.Main.value) {
            Profile tai = immortals.getProfile(identifier);
            if (tai != null) {
                return tai;
            }
        }
        // query from DIM network
        Messenger messenger = Messenger.getInstance();
        messenger.queryProfile(identifier);
        return profile;
    }

    //-------- Relationship

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

    public String getUsername(Object string) {
        return getUsername(getID(string));
    }

    public String getUsername(ID identifier) {
        String username = identifier.name;
        String nickname = getNickname(identifier);
        if (nickname != null && nickname.length() > 0) {
            if (identifier.isUser()) {
                if (username != null && username.length() > 0) {
                    return nickname + " (" + username + ")";
                }
            }
            return nickname;
        } else if (username != null && username.length() > 0) {
            return username;
        }
        // ID only contains address: BTC, ETH, ...
        return identifier.address.toString();
    }

    public String getNickname(ID identifier) {
        assert identifier.isUser();
        Profile profile = getProfile(identifier);
        return profile == null ? null : profile.getName();
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
}
