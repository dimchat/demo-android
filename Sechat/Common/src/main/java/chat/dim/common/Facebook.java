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

import java.util.ArrayList;
import java.util.List;

import chat.dim.AddressNameService;
import chat.dim.Group;
import chat.dim.Immortals;
import chat.dim.User;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.database.AddressNameTable;
import chat.dim.database.ContactTable;
import chat.dim.database.DocumentTable;
import chat.dim.database.GroupTable;
import chat.dim.database.MetaTable;
import chat.dim.database.PrivateKeyTable;
import chat.dim.database.UserTable;
import chat.dim.mkm.Factories;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.NetworkType;

public class Facebook extends chat.dim.Facebook {

    public static long EXPIRES = 30 * 60 * 1000;  // profile expires (30 minutes)
    public static final String EXPIRES_KEY = "expires";

    private Immortals immortals = new Immortals();

    public PrivateKeyTable privateTable = null;
    public MetaTable metaTable = null;
    public DocumentTable docsTable = null;

    public UserTable userTable = null;
    public GroupTable groupTable = null;
    public ContactTable contactTable = null;

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
        ID uid = userTable.getCurrentUser();
        if (uid == null) {
            return null;
        }
        return getUser(uid);
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

    public boolean savePrivateKey(PrivateKey privateKey, ID identifier, String type) {
        return privateTable.savePrivateKey(identifier, privateKey, type);
    }

    //-------- Meta

    @Override
    public boolean saveMeta(Meta meta, ID entity) {
        if (!meta.matches(entity)) {
            // meta not match ID
            return false;
        }
        return metaTable.saveMeta(meta, entity);
    }

    //-------- Profile

    @Override
    public boolean saveDocument(Document profile) {
        if (!isValid(profile)) {
            // profile's signature not match
            return false;
        }
        profile.remove(EXPIRES_KEY);
        return docsTable.saveDocument(profile);
    }

    //-------- Relationship

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

    public boolean removeGroup(ID group) {
        return groupTable.removeGroup(group);
    }

    public boolean containsMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        if (members != null && members.contains(member)) {
            return true;
        }
        ID owner = getOwner(group);
        return owner != null && owner.equals(member);
    }

    public boolean containsAssistant(ID user, ID group) {
        List<ID> assistants = getAssistants(group);
        if (assistants == null) {
            return false;
        }
        return assistants.contains(user);
    }

    //--------

    public String getUsername(Object string) {
        return getUsername(ID.parse(string));
    }
    public String getUsername(ID identifier) {
        String username = identifier.getName();
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
        return identifier.getAddress().toString();
    }

    public String getNickname(Object identifier) {
        return getNickname(ID.parse(identifier));
    }
    public String getNickname(ID identifier) {
        Document profile = getDocument(identifier, "*");
        assert profile != null : "profile object should not be null: " + identifier;
        return profile.getName();
    }
    public String getGroupName(ID identifier) {
        return getNickname(identifier);
    }

    @Override
    protected User createUser(ID identifier) {
        if (isWaitingMeta(identifier)) {
            return null;
        }
        return super.createUser(identifier);
    }

    private boolean isWaitingMeta(ID entity) {
        if (entity.isBroadcast()) {
            return false;
        }
        return getMeta(entity) == null;
    }

    @Override
    protected Group createGroup(ID identifier) {
        if (isWaitingMeta(identifier)) {
            return null;
        }
        return super.createGroup(identifier);
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            // broadcast ID has not meta
            return null;
        }
        // try from database
        Meta meta = metaTable.getMeta(identifier);
        if (meta == null || meta.getKey() == null) {
            // try from immortals
            if (identifier.getType() == NetworkType.MAIN.value) {
                meta = immortals.getMeta(identifier);
                if (meta != null) {
                    metaTable.saveMeta(meta, identifier);
                    return meta;
                }
            }
            meta = null;
        }
        return meta;
    }

    @Override
    public Document getDocument(ID identifier, String type) {
        // try from database
        Document profile = docsTable.getDocument(identifier, type);
        if (isEmpty(profile)) {
            // try from immortals
            if (identifier.getType() == NetworkType.MAIN.value) {
                Document tai = immortals.getDocument(identifier, type);
                if (tai != null) {
                    docsTable.saveDocument(tai);
                    return tai;
                }
            }
            assert profile != null : "profile object should not be null: " + identifier;
        }
        return profile;
    }

    //-------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        List<ID> contacts = contactTable.getContacts(user);
        if (contacts == null || contacts.size() == 0) {
            // try immortals
            contacts = immortals.getContacts(user);
        }
        return contacts;
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        List<DecryptKey> keys = privateTable.getPrivateKeysForDecryption(user);
        if (keys == null || keys.size() == 0) {
            // try immortals
            keys = immortals.getPrivateKeysForDecryption(user);
            if (keys == null || keys.size() == 0) {
                // DIMP v1.0:
                //     decrypt key and the sign key are the same keys
                SignKey sKey = getPrivateKeyForSignature(user);
                if (sKey instanceof DecryptKey) {
                    keys = new ArrayList<>();
                    keys.add((DecryptKey) sKey);
                }
            }
        }
        return keys;
    }

    @Override
    public SignKey getPrivateKeyForSignature(ID user) {
        SignKey key = privateTable.getPrivateKeyForSignature(user);
        if (key == null) {
            // try immortals
            key = immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    @Override
    public SignKey getPrivateKeyForVisaSignature(ID user) {
        SignKey key = privateTable.getPrivateKeyForVisaSignature(user);
        if (key == null) {
            // try immortals
            key = immortals.getPrivateKeyForVisaSignature(user);
        }
        return key;
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

    @Override
    public List<ID> getMembers(ID group) {
        List<ID> members = groupTable.getMembers(group);
        if (members != null && members.size() > 0) {
            return members;
        }
        return super.getMembers(group);
    }

    @Override
    public List<ID> getAssistants(ID group) {
        List<ID> assistants = super.getAssistants(group);
        if (assistants != null && assistants.size() > 0) {
            return assistants;
        }
        // try ANS record
        ID identifier = ID.parse("assistant");
        if (identifier != null) {
            assistants = new ArrayList<>();
            assistants.add(identifier);
            return assistants;
        }
        return null;
    }

    // ANS
    public static AddressNameTable ansTable = null;

    private static final AddressNameService ans = new AddressNameService() {

        @Override
        public ID identifier(String name) {
            ID identifier = super.identifier(name);
            if (identifier != null) {
                return identifier;
            }
            identifier = ansTable.getIdentifier(name);
            if (identifier != null) {
                // FIXME: is reserved name?
                cache(name, identifier);
            }
            return identifier;
        }

        @Override
        public boolean save(String name, ID identifier) {
            if (!super.save(name, identifier)) {
                return false;
            }
            if (identifier == null) {
                return ansTable.removeRecord(name);
            } else {
                return ansTable.addRecord(identifier, name);
            }
        }
    };

    private static final ID.Factory identifierFactory = Factories.idFactory;

    static {

        // load plugins
        chat.dim.Plugins.registerAllPlugins();

        Factories.idFactory = new ID.Factory() {

            @Override
            public ID createID(String name, Address address, String terminal) {
                return identifierFactory.createID(name, address, terminal);
            }

            @Override
            public ID parseID(String identifier) {
                // try ANS record
                ID id = ans.identifier(identifier);
                if (id != null) {
                    return id;
                }
                // parse by original factory
                return identifierFactory.parseID(identifier);
            }
        };
    }
}
