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

import java.util.List;

import chat.dim.common.Facebook;
import chat.dim.crypto.PrivateKey;
import chat.dim.database.Immortals;
import chat.dim.database.SocialNetworkDatabase;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.sechat.Client;

public class AccountDatabase extends SocialNetworkDatabase {
    private static final AccountDatabase ourInstance = new AccountDatabase();
    public static AccountDatabase getInstance() { return ourInstance; }
    private AccountDatabase() {
        super();
        Facebook.getInstance().database = this;
    }

    private Immortals immortals = Immortals.getInstance();

    @Override
    public Meta getMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            return null;
        }
        Meta meta = super.getMeta(identifier);
        if (meta == null) {
            if (identifier.getType().isPerson()) {
                // try immortals
                meta = immortals.getMeta(identifier);
            }
            if (meta == null) {
                // query from DIM network
                Client client = Client.getInstance();
                client.queryMeta(identifier);
            }
        }
        return meta;
    }

    @Override
    public Profile getProfile(ID identifier) {
        Profile profile = super.getProfile(identifier);
        if (profile == null && identifier.getType().isPerson()) {
            profile = immortals.getProfile(identifier);
        }
        return profile;
    }

    @Override
    public PrivateKey getPrivateKeyForSignature(ID user) {
        PrivateKey key = super.getPrivateKeyForSignature(user);
        if (key == null && user.getType().isPerson()) {
            key = immortals.getPrivateKeyForSignature(user);
        }
        return key;
    }

    @Override
    public List<PrivateKey> getPrivateKeysForDecryption(ID user) {
        List<PrivateKey> keys = super.getPrivateKeysForDecryption(user);
        if (keys == null && user.getType().isPerson()) {
            keys = immortals.getPrivateKeysForDecryption(user);
        }
        return keys;
    }

    @Override
    public List<ID> getContacts(ID user) {
        List<ID> contacts = super.getContacts(user);
        if (contacts == null && user.getType().isPerson()) {
            contacts = immortals.getContacts(user);
        }
        return contacts;
    }

    static {
        Facebook facebook = Facebook.getInstance();
        AccountDatabase userDB = AccountDatabase.getInstance();

        // FIXME: test data
        ID hulk = facebook.getID("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");
        ID moki = facebook.getID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");

        LocalUser user = (LocalUser) facebook.getUser(moki);
        userDB.setCurrentUser(user);

        userDB.addContact(hulk, user.identifier);
    }
}
