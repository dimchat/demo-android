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
import java.util.Date;
import java.util.List;

import chat.dim.Anonymous;
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
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;

public class Facebook extends chat.dim.Facebook {

    public static long EXPIRES = 30 * 60 * 1000;  // document expires (30 minutes)
    public static final String EXPIRES_KEY = "expires";

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
            List<ID> list = userTable.allUsers();
            if (list != null) {
                users = new ArrayList<>();
                User user;
                for (ID item : list) {
                    user = getUser(item);
                    if (user == null) {
                        throw new NullPointerException("failed to get local user: " + item);
                    }
                    users.add(user);
                }
            }
        }
        return users;
    }

    @Override
    public User getCurrentUser() {
        ID uid = userTable.getCurrentUser();
        if (uid == null) {
            return super.getCurrentUser();
        }
        return getUser(uid);
    }

    public void setCurrentUser(User user) {
        users = null;
        userTable.setCurrentUser(user.getIdentifier());
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
        return metaTable.saveMeta(meta, entity);
    }

    //-------- Document

    @Override
    public boolean saveDocument(Document doc) {
        if (!checkDocument(doc)) {
            return false;
        }
        doc.remove(EXPIRES_KEY);
        return docsTable.saveDocument(doc);
    }

    public boolean isExpired(Document doc, boolean reset) {
        long now = (new Date()).getTime();
        Number expires = (Number) doc.get(EXPIRES_KEY);
        if (expires == null) {
            // set expired time
            doc.put(EXPIRES_KEY, now + EXPIRES);
            return false;
        } else if (now < expires.longValue()) {
            return false;
        }
        if (reset) {
            // update expired time
            doc.put(EXPIRES_KEY, now + EXPIRES);
        }
        return true;
    }

    //-------- Group

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

    public String getName(ID identifier) {
        // get name from document
        Document doc = getDocument(identifier, "*");
        if (doc != null) {
            String name = doc.getName();
            if (name != null && name.length() > 0) {
                return name;
            }
        }
        // get name from ID
        return Anonymous.getName(identifier);
    }

    @Override
    protected User createUser(ID identifier) {
        if (isWaiting(identifier)) {
            return null;
        } else {
            return super.createUser(identifier);
        }
    }

    private boolean isWaiting(ID entity) {
        return !entity.isBroadcast() && getMeta(entity) == null;
    }

    @Override
    protected Group createGroup(ID identifier) {
        if (isWaiting(identifier)) {
            return null;
        } else {
            return super.createGroup(identifier);
        }
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            // broadcast ID has no meta
            return null;
        } else {
            // try from database
            return metaTable.getMeta(identifier);
        }
    }

    @Override
    public Document getDocument(ID identifier, String type) {
        // try from database
        return docsTable.getDocument(identifier, type);
    }

    //-------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        // try from database
        return contactTable.getContacts(user);
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        // try from database
        List<DecryptKey> keys = privateTable.getPrivateKeysForDecryption(user);
        if (keys == null || keys.size() == 0) {
            // DIMP v1.0:
            //     decrypt key and the sign key are the same keys
            SignKey sKey = getPrivateKeyForSignature(user);
            if (sKey instanceof DecryptKey) {
                keys = new ArrayList<>();
                keys.add((DecryptKey) sKey);
            }
        }
        return keys;
    }

    @Override
    public SignKey getPrivateKeyForSignature(ID user) {
        // try from database
        return privateTable.getPrivateKeyForSignature(user);
    }

    @Override
    public SignKey getPrivateKeyForVisaSignature(ID user) {
        // try from database
        return privateTable.getPrivateKeyForVisaSignature(user);
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

    private static final AddressNameServer ans = new AddressNameServer() {

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
            if (!cache(name, identifier)) {
                return false;
            }
            if (identifier == null) {
                return ansTable.removeRecord(name);
            } else {
                return ansTable.addRecord(identifier, name);
            }
        }
    };

    private static final ID.Factory identifierFactory;

    static {

        // load plugins
        chat.dim.Plugins.registerAllPlugins();

        identifierFactory = ID.getFactory();
        ID.setFactory(new ID.Factory() {

            @Override
            public ID generateID(Meta meta, int type, String terminal) {
                return identifierFactory.generateID(meta, type, terminal);
            }

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
        });
    }
}
