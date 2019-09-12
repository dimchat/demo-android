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

import chat.dim.client.Facebook;
import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.EntityDataSource;
import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.NetworkType;
import chat.dim.mkm.UserDataSource;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;

public class SocialNetworkDatabase implements EntityDataSource, UserDataSource, GroupDataSource {
    private static final SocialNetworkDatabase ourInstance = new SocialNetworkDatabase();
    public static SocialNetworkDatabase getInstance() { return ourInstance; }
    private SocialNetworkDatabase() {
    }

    private static Immortals immortals = Immortals.getInstance();

    public void reloadData() {
        UserTable.loadUsers();
        ID user = UserTable.getCurrentUser();
        if (user == null) {
            return;
        }
        // reload contacts for current user
        ContactTable.reloadContacts(user);
        // reload conversation database
        ConversationDatabase.getInstance().reloadData(user);
    }

    public LocalUser getCurrentUser() {
        Facebook facebook = Facebook.getInstance();
        return (LocalUser) facebook.getUser(UserTable.getCurrentUser());
    }

    public void setCurrentUser(LocalUser user) {
        UserTable.setCurrentUser(user.identifier);
    }

    public List<ID> allUsers() {
        return UserTable.allUsers();
    }

    public boolean addUser(ID user) {
        return UserTable.addUser(user);
    }

    public boolean removeUser(ID user) {
        return UserTable.removeUser(user);
    }

    public boolean addContact(ID contact, ID user) {
        return ContactTable.addContact(contact, user);
    }

    public boolean removeContact(ID contact, ID user) {
        return ContactTable.removeContact(contact, user);
    }

    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        return UserTable.savePrivateKey(privateKey, identifier);
    }

    public boolean saveMeta(Meta meta, ID identifier) {
        return MetaTable.saveMeta(meta, identifier);
    }

    public boolean saveProfile(Profile profile) {
        if (!verifyProfile(profile)) {
            return false;
        }
        return ProfileTable.saveProfile(profile);
    }

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

    public boolean saveAnsRecord(String name, ID identifier) {
        return AddressNameTable.saveRecord(name, identifier);
    }

    public ID ansRecord(String name) {
        return AddressNameTable.record(name);
    }

    public Set<String> ansNames(String identifier) {
        return AddressNameTable.names(identifier);
    }

    //---- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        Meta meta = MetaTable.getMeta(identifier);
        if (meta == null && identifier.getType().isPerson()) {
            meta = immortals.getMeta(identifier);
        }
        return meta;
    }

    @Override
    public Profile getProfile(ID identifier) {
        Profile profile = ProfileTable.getProfile(identifier);
        if (profile == null && identifier.getType().isPerson()) {
            profile = immortals.getProfile(identifier);
        }
        return profile;
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = UserTable.getPrivateKeyForSignature(user);
        if (key == null && user.getType().isPerson()) {
            key = immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        List<PrivateKey> keys = UserTable.getPrivateKeysForDecryption(user);
        if (keys == null && user.getType().isPerson()) {
            keys = immortals.getPrivateKeysForDecryption(user);
        }
        return keys;
    }

    @Override
    public List<ID> getContacts(ID user) {
        return ContactTable.getContacts(user);
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        return GroupTable.getFounder(group);
    }

    @Override
    public ID getOwner(ID group) {
        return GroupTable.getOwner(group);
    }

    @Override
    public List<ID> getMembers(ID group) {
        return GroupTable.getMembers(group);
    }
}
