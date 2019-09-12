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
package chat.dim.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.core.Barrack;
import chat.dim.crypto.PrivateKey;
import chat.dim.database.ConversationDatabase;
import chat.dim.database.SocialNetworkDatabase;
import chat.dim.group.Chatroom;
import chat.dim.group.Polylogue;
import chat.dim.mkm.EntityDataSource;
import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.NetworkType;
import chat.dim.mkm.Profile;
import chat.dim.mkm.User;
import chat.dim.mkm.Group;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.UserDataSource;
import chat.dim.network.Station;

public class Facebook extends Barrack {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    SocialNetworkDatabase userDB = SocialNetworkDatabase.getInstance();
    ConversationDatabase msgDB = ConversationDatabase.getInstance();

    // delegates
    public EntityDataSource entityDataSource = userDB;
    public UserDataSource userDataSource   = userDB;
    public GroupDataSource groupDataSource  = userDB;

    //---- Private Key

    public boolean saveProvateKey(PrivateKey privateKey, ID identifier) {
        return userDB.savePrivateKey(privateKey, identifier);
    }

    //---- Meta

    public boolean saveMeta(Meta meta, ID entity) {
        return userDB.saveMeta(meta, entity);
    }

    //---- Profile

    public boolean saveProfile(Profile profile) {
        return userDB.saveProfile(profile);
    }

    public boolean verifyProfile(Profile profile) {
        return userDB.verifyProfile(profile);
    }

    public String getNickname(ID identifier) {
        assert identifier.getType().isUser();
        User user = getUser(identifier);
        return user == null ? null : user.getName();
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
        ID identifier = userDB.ansRecord((String) string);
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
        meta = entityDataSource.getMeta(entity);
        if (meta != null && cacheMeta(meta, entity)) {
            return meta;
        }
        return null;
    }

    @Override
    public Profile getProfile(ID entity) {
        return entityDataSource.getProfile(entity);
    }

    //-------- UserDataSource

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        return userDataSource.getPrivateKeyForSignature(user);
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        return userDataSource.getPrivateKeysForDecryption(user);
    }

    @Override
    public List<ID> getContacts(ID user) {
        return userDataSource.getContacts(user);
    }

    //-------- GroupDataSource

    @Override
    public ID getFounder(ID group) {
        ID founder = groupDataSource.getFounder(group);
        if (founder != null) {
            return founder;
        }
        // check each member's public key with group meta
        Meta gMeta = getMeta(group);
        List<ID> members = groupDataSource.getMembers(group);
        if (gMeta == null || members == null) {
            //throw new NullPointerException("failed to get group info: " + gMeta + ", " + members);
            return null;
        }
        for (ID member : members) {
            Meta meta = getMeta(member);
            if (meta == null) {
                // TODO: query meta for this member from DIM network
                continue;
            }
            if (gMeta.matches(meta.key)) {
                // if public key matched, means the group is created by this member
                return member;
            }
        }
        return null;
    }

    @Override
    public ID getOwner(ID group) {
        return groupDataSource.getOwner(group);
    }

    @Override
    public List<ID> getMembers(ID group) {
        return groupDataSource.getMembers(group);
    }
}
