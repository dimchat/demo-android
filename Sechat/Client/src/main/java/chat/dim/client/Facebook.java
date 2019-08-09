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

import chat.dim.core.Barrack;
import chat.dim.crypto.PrivateKey;
import chat.dim.group.Chatroom;
import chat.dim.group.Polylogue;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.mkm.entity.NetworkType;
import chat.dim.mkm.entity.Profile;
import chat.dim.network.Station;

public class Facebook extends Barrack {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    public FacebookDelegate delegate = null;

    public boolean savePrivateKey(PrivateKey privateKey, ID identifier) {
        if (delegate == null) {
            throw new NullPointerException("Facebook delegate not set yet");
        }
        return delegate.savePrivateKey(privateKey, identifier);
    }

    public boolean saveProfile(Profile profile) {
        if (!verifyProfile(profile)) {
            throw new IllegalArgumentException("profile error: " + profile);
        }
        if (delegate == null) {
            throw new NullPointerException("Facebook delegate not set yet");
        }
        return delegate.saveProfile(profile);
    }

    private boolean verifyProfile(Profile profile) {
        if (profile == null) {
            return false;
        } else if (profile.isValid()) {
            // already verified
            return true;
        }
        ID identifier = profile.identifier;
        NetworkType type = identifier.getType();
        Meta meta = null;
        if (type.isCommunicator()) {
            // verify with account's meta.key
            meta = getMeta(identifier);
        } else if (type.isGroup()) {
            // verify with group owner's meta.key
            Group group = getGroup(identifier);
            if (group != null) {
                meta = getMeta(group.getOwner());
            }
        }
        return meta != null && profile.verify(meta.key);
    }

    //-------- SocialNetworkDataSource

    @Override
    public Account getAccount(ID identifier) {
        Account account = super.getAccount(identifier);
        if (account != null) {
            return account;
        }
        // check meta
        Meta meta = getMeta(identifier);
        if (meta == null) {
            throw new NullPointerException("meta not found: " + identifier);
        }
        // create it with type
        NetworkType type = identifier.getType();
        if (type.isStation()) {
            account = new Station(identifier);
        } else if (type.isPerson()) {
            account = new Account(identifier);
        }
        assert account != null;
        cacheAccount(account);
        return account;
    }

    @Override
    public User getUser(ID identifier) {
        if (!identifier.getType().isPerson()) {
            return null;
        }
        User user = super.getUser(identifier);
        if (user != null) {
            return user;
        }
        // check meta and private key
        Meta meta = getMeta(identifier);
        PrivateKey key = getPrivateKeyForSignature(identifier);
        if (meta == null || key == null) {
            throw new NullPointerException("meta/private key not found: " + identifier);
        }
        // create it
        user = new User(identifier);
        cacheUser(user);
        return user;
    }

    @Override
    public Group getGroup(ID identifier) {
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
        cacheGroup(group);
        return group;
    }
}
