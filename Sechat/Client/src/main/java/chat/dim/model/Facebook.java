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
import java.util.Map;

import chat.dim.GroupManager;
import chat.dim.mkm.BroadcastAddress;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.network.FtpServer;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.utils.Log;

public final class Facebook extends chat.dim.common.Facebook {

    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    public String getAvatar(ID identifier) {
        String url = null;
        Document profile = getDocument(identifier, Document.PROFILE);
        if (!isEmpty(profile)) {
            if (profile instanceof UserProfile) {
                url = ((UserProfile) profile).getAvatar();
            } else {
                url = (String) profile.getProperty("avatar");
            }
        }
        if (url == null || url.length() == 0) {
            return null;
        }
        FtpServer ftp = FtpServer.getInstance();
        return ftp.downloadAvatar(url, identifier);
    }

    //-------- Meta

    @Override
    public boolean saveMeta(Meta meta, ID entity) {
        if (!super.saveMeta(meta, entity)) {
            return false;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("ID", entity);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.MetaSaved, this, info);
        return true;
    }

    //-------- Profile

    @Override
    public boolean saveDocument(Document profile) {
        if (!super.saveDocument(profile)) {
            return false;
        }
        ID entity = getID(profile.getIdentifier());

        Map<String, Object> info = new HashMap<>();
        info.put("ID", entity);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.ProfileUpdated, this, info);
        return true;
    }

    //-------- Contacts

    @Override
    public boolean addContact(ID contact, ID user) {
        if (!super.addContact(contact, user)) {
            return false;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("action", "add");
        info.put("ID", contact);
        info.put("user", user);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.ContactsUpdated, this, info);
        return true;
    }

    @Override
    public boolean removeContact(ID contact, ID user) {
        if (!super.removeContact(contact, user)) {
            return false;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("action", "remove");
        info.put("ID", contact);
        info.put("user", user);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.ContactsUpdated, this, info);
        return true;
    }

    //-------- Relationship

    @Override
    public boolean addMember(ID member, ID group) {
        if (!super.addMember(member, group)) {
            return false;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("action", "add");
        info.put("ID", member);
        info.put("group", group);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.MembersUpdated, this, info);
        return true;
    }

    @Override
    public boolean removeMember(ID member, ID group) {
        if (!super.removeMember(member, group)) {
            return false;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("action", "remove");
        info.put("ID", member);
        info.put("group", group);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.MembersUpdated, this, info);
        return true;
    }

    @Override
    public boolean saveMembers(List<ID> members, ID group) {
        if (!super.saveMembers(members, group)) {
            return false;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("members", members);
        info.put("group", group);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.MembersUpdated, this, info);
        return true;
    }

    @Override
    public boolean removeGroup(ID group) {
        if (!super.removeGroup(group)) {
            return false;
        }

        Map<String, Object> info = new HashMap<>();
        info.put("group", group);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.GroupRemoved, this, info);
        return true;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        if (identifier.getAddress() instanceof BroadcastAddress) {
            // broadcast ID has not meta
            return null;
        }
        // try from database
        Meta meta = super.getMeta(identifier);
        if (meta == null) {
            // query from DIM network
            Messenger messenger = Messenger.getInstance();
            messenger.queryMeta(identifier);
        }
        return meta;
    }

    @Override
    public Document getDocument(ID identifier, String type) {
        // try from database
        Document doc = super.getDocument(identifier, type);
        if (isEmpty(doc)/* && isExpired(profile)*/) {
            // update EXPIRES value
            long now = (new Date()).getTime();
            doc.put(EXPIRES_KEY, now + EXPIRES);
            // query from DIM network
            Messenger messenger = Messenger.getInstance();
            messenger.queryProfile(identifier);
        }
        return doc;
    }

    //-------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        List<ID> contacts = super.getContacts(user);
        if (contacts == null || contacts.size() == 0) {
            contacts = Configuration.getInstance().getDefaultContacts();
            if (contacts != null) {
                // save default contacts
                for (ID item : contacts) {
                    addContact(item, user);
                }
            }
        }
        return contacts;
    }

    //-------- GroupDataSource

    @Override
    public List<ID> getMembers(ID group) {
        List<ID> members = super.getMembers(group);
        if (members == null || members.size() == 0) {
            // query from group assistants
            Log.info("querying members: " + group);
            GroupManager gm = new GroupManager(group);
            gm.query();
        }
        return members;
    }

    @Override
    public boolean existsMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        if (members != null && members.contains(member)) {
            return true;
        }
        ID owner = getOwner(group);
        return owner != null && owner.equals(member);
    }

    @Override
    public List<ID> getAssistants(ID group) {
        List<ID> assistants = new ArrayList<>();
        // dev
        assistants.add(getID("assistant@2PpB6iscuBjA15oTjAsiswoX9qis5V3c1Dq"));
        // desktop.dim.chat
        assistants.add(getID("assistant@4WBSiDzg9cpZGPqFrQ4bHcq4U5z9QAQLHS"));
        return assistants;
        //return super.getAssistants(group);
    }
}
