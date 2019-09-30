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
package chat.dim.database;

import java.util.List;
import java.util.Set;

import chat.dim.common.Facebook;
import chat.dim.common.SocialNetworkDataSource;
import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.NetworkType;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;

public class SocialNetworkDatabase implements SocialNetworkDataSource {

    private PrivateTable privateTable = new PrivateTable();
    private MetaTable metaTable = new MetaTable();
    private ProfileTable profileTable = new ProfileTable();

    private AddressNameTable ansTable = new AddressNameTable();

    private UserTable userTable = new UserTable();
    private GroupTable groupTable = new GroupTable();
    private ContactTable contactTable = new ContactTable();

    @Override
    public LocalUser getCurrentUser() {
        Facebook facebook = Facebook.getInstance();
        return (LocalUser) facebook.getUser(userTable.getCurrentUser());
    }

    @Override
    public void setCurrentUser(LocalUser user) {
        userTable.setCurrentUser(user.identifier);
    }

    @Override
    public List<ID> allUsers() {
        return userTable.allUsers();
    }

    @Override
    public boolean addUser(ID user) {
        return userTable.addUser(user);
    }

    @Override
    public boolean removeUser(ID user) {
        return userTable.removeUser(user);
    }

    @Override
    public boolean addContact(ID contact, ID user) {
        return contactTable.addContact(contact, user);
    }

    @Override
    public boolean removeContact(ID contact, ID user) {
        return contactTable.removeContact(contact, user);
    }

    @Override
    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        return privateTable.savePrivateKey(privateKey, identifier);
    }

    @Override
    public boolean saveMeta(Meta meta, ID identifier) {
        return metaTable.saveMeta(meta, identifier);
    }

    @Override
    public boolean saveProfile(Profile profile) {
        if (!verifyProfile(profile)) {
            return false;
        }
        return profileTable.saveProfile(profile);
    }

    @Override
    public boolean verifyProfile(Profile profile) {
        if (profile == null) {
            return false;
        } else if (profile.isValid()) {
            return true;
        }
        ID identifier = profile.identifier;
        assert identifier.isValid();
        NetworkType type = identifier.getType();
        if (type.isUser() || type.value == NetworkType.Polylogue.value) {
            // if this is a user profile,
            //     verify it with the user's meta.key
            // else if this is a polylogue profile,
            //     verify it with the founder's meta.key (which equals to the group's meta.key)
            Meta meta = getMeta(identifier);
            return meta != null && profile.verify(meta.key);
        }
        throw new UnsupportedOperationException("unsupported profile ID: " + profile);
    }

    // Address Name Service

    @Override
    public boolean saveAnsRecord(String name, ID identifier) {
        return ansTable.saveRecord(name, identifier);
    }

    @Override
    public ID ansRecord(String name) {
        return ansTable.record(name);
    }

    @Override
    public Set<String> ansNames(String identifier) {
        return ansTable.names(identifier);
    }

    //---- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        return metaTable.getMeta(identifier);
    }

    @Override
    public Profile getProfile(ID identifier) {
        return profileTable.getProfile(identifier);
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        return privateTable.getPrivateKeyForSignature(user);
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        return privateTable.getPrivateKeysForDecryption(user);
    }

    @Override
    public List<ID> getContacts(ID user) {
        return contactTable.getContacts(user);
    }

    //-------- GroupDataSource

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
    public boolean existsMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        for (ID item : members) {
            if (item.equals(member)) {
                return true;
            }
        }
        ID owner = getOwner(group);
        return owner == null || owner.equals(member);
    }
}
