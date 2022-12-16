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
package chat.dim;

import java.util.ArrayList;
import java.util.List;

import chat.dim.crypto.PrivateKey;
import chat.dim.database.AddressNameTable;
import chat.dim.dbi.AccountDBI;
import chat.dim.http.HTTPClient;
import chat.dim.protocol.Address;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.Visa;
import chat.dim.type.Pair;

public final class SharedFacebook extends CommonFacebook {

    private final List<ID> groupAssistants = new ArrayList<>();

    public SharedFacebook(AccountDBI db) {
        super(db);
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

    /**
     *  Get avatar for user
     *
     * @param user - user ID
     * @return cache path & remote URL
     */
    public Pair<String, String> getAvatar(ID user) {
        String url = null;
        Document doc = getDocument(user, "*");
        if (doc != null) {
            if (doc instanceof Visa) {
                url = ((Visa) doc).getAvatar();
            } else {
                url = (String) doc.getProperty("avatar");
            }
        }
        if (url == null || url.length() == 0) {
            return new Pair<>(null, null);
        }
        HTTPClient http = HTTPClient.getInstance();
        String path = http.download(url);
        return new Pair<>(path, url);
    }

    public boolean saveContacts(List<ID> contacts, ID user) {
        AccountDBI db = getDatabase();
        return db.saveContacts(contacts, user);
    }

    public boolean savePrivateKey(PrivateKey key, String type, ID user) {
        AccountDBI db = getDatabase();
        return db.savePrivateKey(key, type, user);
    }

    public boolean addUser(ID user) {
        AccountDBI db = getDatabase();
        List<ID> allUsers = db.getLocalUsers();
        if (allUsers == null) {
            allUsers = new ArrayList<>();
        } else if (allUsers.contains(user)) {
            // already exists
            return false;
        }
        allUsers.add(user);
        return db.saveLocalUsers(allUsers);
    }

    public boolean removeUser(ID user) {
        AccountDBI db = getDatabase();
        List<ID> allUsers = db.getLocalUsers();
        if (allUsers == null || !allUsers.contains(user)) {
            // not exists
            return false;
        }
        allUsers.add(user);
        return db.saveLocalUsers(allUsers);
    }

    //-------- Contacts

    public boolean addContact(ID contact, ID user) {
        // TODO: save into db
        List<ID> allContatcs = getContacts(user);
        if (allContatcs == null) {
            allContatcs = new ArrayList<>();
        }
        allContatcs.add(contact);
        return saveContacts(allContatcs, user);
    }

    public boolean removeContact(ID contact, ID user) {
        // TODO: save into db
        List<ID> allContatcs = getContacts(user);
        if (allContatcs == null || !allContatcs.contains(contact)) {
            return false;
        }
        allContatcs.remove(contact);
        return saveContacts(allContatcs, user);
    }

    //-------- Members

    public void addMember(ID member, ID group) {
        assert member.isUser() && group.isGroup() : "ID error: " + member + ", " + group;
        // TODO: save into db
        List<ID> allMembers = getMembers(group);
        if (allMembers == null) {
            allMembers = new ArrayList<>();
        }
        allMembers.add(member);
        saveMembers(allMembers, group);
    }

    public boolean containsMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        if (members != null && members.contains(member)) {
            return true;
        }
        ID owner = getOwner(group);
        return owner != null && owner.equals(member);
    }

    public boolean removeGroup(ID group) {
        // TODO:
        //return groupTable.removeGroup(group);
        return false;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        List<ID> assistants = super.getAssistants(group);
        if (assistants != null && assistants.size() > 0) {
            return assistants;
        }
        // get from global setting
        if (groupAssistants.size() > 0) {
            return groupAssistants;
        }
        // get from ANS
        assistants = new ArrayList<>();
        ID bot = ID.parse("assistant");
        if (bot != null) {
            assistants.add(bot);
        }
        return assistants;
    }
    public void addAssistant(ID assistant) {
        assert assistant != null : "bot ID empty";
        for (ID item : groupAssistants) {
            if (item.equals(assistant)) {
                return;
            }
        }
        ID bot = ID.parse("assistant");
        if (bot != null && bot.equals(assistant)) {
            groupAssistants.add(0, assistant);
        } else {
            groupAssistants.add(assistant);
        }
    }

    public boolean containsAssistant(ID user, ID group) {
        List<ID> assistants = getAssistants(group);
        if (assistants == null) {
            return false;
        }
        return assistants.contains(user);
    }

    //
    //  ANS
    //

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
