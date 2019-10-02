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

import chat.dim.core.Barrack;
import chat.dim.crypto.PrivateKey;
import chat.dim.group.Chatroom;
import chat.dim.group.Polylogue;
import chat.dim.mkm.Address;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.NetworkType;
import chat.dim.mkm.Profile;
import chat.dim.mkm.User;
import chat.dim.mkm.Group;
import chat.dim.mkm.LocalUser;
import chat.dim.network.Station;

public class Facebook extends Barrack {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    public SocialNetworkDataSource database = null;

    //---- Private Key

    public boolean saveProvateKey(PrivateKey privateKey, ID identifier) {
        return database.savePrivateKey(privateKey, identifier);
    }

    //---- Meta

    public boolean saveMeta(Meta meta, ID entity) {
        return database.saveMeta(meta, entity);
    }

    //---- Profile

    public boolean saveProfile(Profile profile) {
        return database.saveProfile(profile);
    }

    public boolean verifyProfile(Profile profile) {
        return database.verifyProfile(profile);
    }

    //---- Relationship

    public boolean saveContacts(List<ID> contacts, ID user) {
        return database.saveContacts(contacts, user);
    }

    public boolean addMember(ID member, ID group) {
        return database.addMember(member, group);
    }

    public boolean removeMember(ID member, ID group) {
        return database.removeMember(member, group);
    }

    public boolean saveMembers(List<ID> members, ID group) {
        return database.saveMembers(members, group);
    }

    //----

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

    public ID getID(Address address) {
        ID identifier = new ID(null, address);
        Meta meta = database.getMeta(identifier);
        if (meta == null) {
            // failed to get meta for this ID
            return null;
        }
        String seed = meta.seed;
        if (seed == null) {
            return identifier;
        }
        identifier = new ID(seed, address);
        cacheID(identifier);
        return identifier;
    }

    //-------- SocialNetworkDataSource

    @Override
    public ID getID(Object string) {
        if (string == null) {
            return null;
        } else if (string instanceof ID) {
            return (ID) string;
        }
        assert string instanceof String;
        // try ANS record
        ID identifier = database.ansRecord((String) string);
        if (identifier != null) {
            return identifier;
        }
        // get from barrack
        return super.getID(string);
    }

    @Override
    public User getUser(ID identifier) {
        // get from barrack cache
        User user = super.getUser(identifier);
        if (user != null) {
            return user;
        }
        // check meta and private key
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        NetworkType type = identifier.getType();
        if (type.isPerson()) {
            PrivateKey key = getPrivateKeyForSignature(identifier);
            if (key == null) {
                user = new User(identifier);
            } else {
                user = new LocalUser(identifier);
            }
        } else if (type.isStation()) {
            // FIXME: prevent station to be erased from memory cache
            user = new Station(identifier);
        } else {
            throw new UnsupportedOperationException("unsupported user type: " + type);
        }
        // cache it in barrack
        cacheUser(user);
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
        // get from barrack cache
        Group group = super.getGroup(identifier);
        if (group != null) {
            return group;
        }
        // check meta
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        // create it with type
        NetworkType type = identifier.getType();
        if (type == NetworkType.Polylogue) {
            group = new Polylogue(identifier);
        } else if (type == NetworkType.Chatroom) {
            group = new Chatroom(identifier);
        }
        assert group != null;
        // cache it in barrack
        cacheGroup(group);
        return group;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID entity) {
        Meta meta = super.getMeta(entity);
        if (meta != null) {
            return meta;
        }
        meta = database.getMeta(entity);
        if (meta != null && cacheMeta(meta, entity)) {
            return meta;
        }
        return null;
    }

    @Override
    public Profile getProfile(ID entity) {
        return database.getProfile(entity);
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        return database.getPrivateKeyForSignature(user);
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        return database.getPrivateKeysForDecryption(user);
    }

    @Override
    public List<ID> getContacts(ID user) {
        return database.getContacts(user);
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        // get from database
        ID founder = database.getFounder(group);
        if (founder != null) {
            return founder;
        }
        return super.getFounder(group);
    }

    @Override
    public ID getOwner(ID group) {
        // get from database
        ID owner = database.getOwner(group);
        if (owner != null) {
            return owner;
        }
        return super.getOwner(group);
    }

    @Override
    public List<ID> getMembers(ID group) {
        // get from database
        List<ID> members = database.getMembers(group);
        if (members != null) {
            return members;
        }
        return super.getMembers(group);
    }

    public boolean isFounder(ID member, ID group) {
        // check member's public key with group's meta.key
        Meta gMeta = getMeta(group);
        if (gMeta == null) {
            throw new NullPointerException("failed to get meta for group: " + group);
        }
        Meta meta = getMeta(member);
        if (meta == null) {
            throw new NullPointerException("failed to get meta for member: " + member);
        }
        return gMeta.matches(meta.key);
    }

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
