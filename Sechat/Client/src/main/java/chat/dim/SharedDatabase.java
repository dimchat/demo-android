package chat.dim;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.database.ContactTable;
import chat.dim.database.DocumentTable;
import chat.dim.database.GroupTable;
import chat.dim.database.LoginTable;
import chat.dim.database.MetaTable;
import chat.dim.database.MsgKeyTable;
import chat.dim.database.PrivateKeyTable;
import chat.dim.database.ProviderTable;
import chat.dim.database.UserTable;
import chat.dim.dbi.AccountDBI;
import chat.dim.dbi.MessageDBI;
import chat.dim.dbi.ProviderInfo;
import chat.dim.dbi.SessionDBI;
import chat.dim.dbi.StationInfo;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Document;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.type.Pair;

public class SharedDatabase implements AccountDBI, MessageDBI, SessionDBI, UserTable {

    public PrivateKeyTable privateKeyTable;
    public MetaTable metaTable;
    public DocumentTable documentTable;
    public UserTable userTable;
    public ContactTable contactTable;
    public GroupTable groupTable;

    public MsgKeyTable msgKeyTable;

    public LoginTable loginTable;
    public ProviderTable providerTable;

    //
    //  User Table
    //

    @Override
    public boolean addUser(ID user) {
        return userTable.addUser(user);
    }

    @Override
    public boolean removeUser(ID user) {
        return userTable.removeUser(user);
    }

    @Override
    public void setCurrentUser(ID user) {
        userTable.setCurrentUser(user);
    }

    @Override
    public ID getCurrentUser() {
        return userTable.getCurrentUser();
    }

    //
    //  Account DBI
    //

    @Override
    public boolean saveDocument(Document doc) {
        ID identifier = doc.getIdentifier();
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not exists: " + identifier);
        }
        if (!(doc.isValid() || doc.verify(meta.getPublicKey()))) {
            throw new VerifyError("document error: " + doc);
        }
        boolean ok = documentTable.saveDocument(doc);
        if (ok) {
            Map<String, Object> info = doc.toMap();
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.DocumentUpdated, this, info);
        }
        return ok;
    }

    @Override
    public Document getDocument(ID entity, String type) {
        return documentTable.getDocument(entity, type);
    }

    @Override
    public ID getFounder(ID group) {
        return groupTable.getFounder(group);
    }

    @Override
    public ID getOwner(ID group) {
        return groupTable.getOwner(group);
    }

    @Override
    public List<ID> getMembers(ID group) {
        return groupTable.getMembers(group);
    }

    @Override
    public boolean saveMembers(List<ID> members, ID group) {
        boolean ok = groupTable.saveMembers(members, group);
        if (ok) {
            Map<String, Object> info = new HashMap<>();
            info.put("action", "update");
            info.put("members", members);
            info.put("group", group);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.MembersUpdated, this, info);
        }
        return ok;
    }

    public boolean addMember(ID member, ID group) {
        boolean ok = groupTable.addMember(member, group);
        if (ok) {
            Map<String, Object> info = new HashMap<>();
            info.put("action", "add");
            info.put("member", member);
            info.put("group", group);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.MembersUpdated, this, info);
        }
        return ok;
    }

    public boolean removeMember(ID member, ID group) {
        boolean ok = groupTable.removeMember(member, group);
        if (ok) {
            Map<String, Object> info = new HashMap<>();
            info.put("action", "remove");
            info.put("member", member);
            info.put("group", group);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.MembersUpdated, this, info);
        }
        return ok;
    }

    public boolean removeGroup(ID group) {
        boolean ok = groupTable.removeGroup(group);
        if (ok) {
            Map<String, Object> info = new HashMap<>();
            info.put("action", "remove");
            info.put("group", group);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.GroupRemoved, this, info);
        }
        return ok;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        return groupTable.getAssistants(group);
    }

    @Override
    public boolean saveAssistants(List<ID> bots, ID group) {
        return groupTable.saveAssistants(bots, group);
    }

    @Override
    public List<ID> getAdministrators(ID group) {
        return groupTable.getAdministrators(group);
    }

    @Override
    public boolean saveAdministrators(List<ID> members, ID group) {
        return groupTable.saveAdministrators(members, group);
    }

    //
    //  Group History DBI
    //

    @Override
    public boolean saveGroupHistory(GroupCommand content, ReliableMessage rMsg, ID group) {
        return false;
    }

    @Override
    public List<Pair<GroupCommand, ReliableMessage>> getGroupHistories(ID group) {
        return null;
    }

    @Override
    public Pair<ResetCommand, ReliableMessage> getResetCommandMessage(ID identifier) {
        return null;
    }

    @Override
    public boolean clearGroupMemberHistories(ID group) {
        return false;
    }

    @Override
    public boolean clearGroupAdminHistories(ID group) {
        return false;
    }

    //
    //  Meta DBI
    //

    @Override
    public boolean saveMeta(Meta meta, ID entity) {
        if (!meta.matchIdentifier(entity)) {
            throw new VerifyError("meta not match: " + entity + " => " + meta);
        }
        boolean ok = metaTable.saveMeta(meta, entity);
        if (ok) {
            Map<String, Object> info = new HashMap<>();
            info.put("ID", entity);
            info.put("meta", meta);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.MetaSaved, this, info);
        }
        return ok;
    }

    @Override
    public Meta getMeta(ID entity) {
        return metaTable.getMeta(entity);
    }

    @Override
    public boolean savePrivateKey(PrivateKey key, String type, ID user) {
        return privateKeyTable.savePrivateKey(key, type, user);
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        return privateKeyTable.getPrivateKeysForDecryption(user);
    }

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        return privateKeyTable.getPrivateKeyForSignature(user);
    }

    @Override
    public PrivateKey getPrivateKeyForVisaSignature(ID user) {
        return privateKeyTable.getPrivateKeyForVisaSignature(user);
    }

    @Override
    public List<ID> getLocalUsers() {
        return userTable.getLocalUsers();
    }

    @Override
    public boolean saveLocalUsers(List<ID> users) {
        return userTable.saveLocalUsers(users);
    }

    @Override
    public List<ID> getContacts(ID user) {
        //return userTable.getContacts(user);
        return contactTable.getContacts(user);
    }

    @Override
    public boolean saveContacts(List<ID> contacts, ID user) {
        //return userTable.saveContacts(contacts, user);
        return contactTable.saveContacts(contacts, user);
    }

    public boolean addContact(ID contact, ID user) {
        boolean ok = contactTable.addContact(contact, user);
        if (ok) {
            Map<String, Object> info = new HashMap<>();
            info.put("action", "add");
            info.put("contact", contact);
            info.put("user", user);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.ContactsUpdated, this, info);
        }
        return ok;
    }

    public boolean removeContact(ID contact, ID user) {
        boolean ok = contactTable.removeContact(contact, user);
        if (ok) {
            Map<String, Object> info = new HashMap<>();
            info.put("action", "remove");
            info.put("contact", contact);
            info.put("user", user);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.ContactsUpdated, this, info);
        }
        return ok;
    }

    //
    //  Message DBI
    //

    @Override
    public SymmetricKey getCipherKey(ID sender, ID receiver, boolean generate) {
        return msgKeyTable.getCipherKey(sender, receiver, generate);
    }

    @Override
    public void cacheCipherKey(ID sender, ID receiver, SymmetricKey key) {
        msgKeyTable.cacheCipherKey(sender, receiver, key);
    }

    @Override
    public Map<String, Object> getGroupKeys(ID group, ID sender) {
        return null;
    }

    @Override
    public boolean saveGroupKeys(ID group, ID sender, Map<String, Object> keys) {
        return false;
    }

    @Override
    public Pair<List<ReliableMessage>, Integer> getReliableMessages(ID receiver, int start, int limit) {
        // TODO:
        return null;
    }

    @Override
    public boolean cacheReliableMessage(ID receiver, ReliableMessage rMsg) {
        // TODO:
        return false;
    }

    @Override
    public boolean removeReliableMessage(ID receiver, ReliableMessage rMsg) {
        // TODO:
        return false;
    }

    //
    //  Session DBI
    //

    @Override
    public Pair<LoginCommand, ReliableMessage> getLoginCommandMessage(ID identifier) {
        return loginTable.getLoginCommandMessage(identifier);
    }

    @Override
    public boolean saveLoginCommandMessage(ID identifier, LoginCommand content, ReliableMessage rMsg) {
        return loginTable.saveLoginCommandMessage(identifier, content, rMsg);
    }

    @Override
    public List<ProviderInfo> allProviders() {
        return null;
    }

    @Override
    public boolean addProvider(ID identifier, int chosen) {
        return false;
    }

    @Override
    public boolean updateProvider(ID identifier, int chosen) {
        return false;
    }

    @Override
    public boolean removeProvider(ID identifier) {
        return false;
    }

    @Override
    public List<StationInfo> allStations(ID provider) {
        return null;
    }

    @Override
    public boolean addStation(ID identifier, String host, int port, ID provider, int chosen) {
        return false;
    }

    @Override
    public boolean updateStation(ID identifier, String host, int port, ID provider, int chosen) {
        return false;
    }

    @Override
    public boolean removeStation(String host, int port, ID provider) {
        return false;
    }

    @Override
    public boolean removeStations(ID provider) {
        return false;
    }
}
