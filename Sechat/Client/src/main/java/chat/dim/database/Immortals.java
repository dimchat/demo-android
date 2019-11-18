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

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.crypto.impl.PrivateKeyImpl;
import chat.dim.filesys.Resource;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.Address;
import chat.dim.mkm.UserDataSource;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.utils.Log;

public class Immortals implements UserDataSource {
    private static final Immortals ourInstance = new Immortals();
    public static Immortals getInstance() { return ourInstance; }
    private Immortals() {
    }

    private final Map<Address, Meta>    metaMap       = new HashMap<>();
    private Map<Address, PrivateKey>    privateKeyMap = new HashMap<>();
    private final Map<Address, Profile> profileMap    = new HashMap<>();

    @SuppressWarnings("unchecked")
    private Profile getProfile(Map dictionary, ID identifier, PrivateKey privateKey) {
        Profile profile;
        String profile_data = (String) dictionary.get("data");
        if (profile_data == null) {
            profile = new Profile(identifier);
            // set name
            String name = (String) dictionary.get("name");
            if (name == null) {
                List<String> names = (List<String>) dictionary.get("names");
                if (names != null) {
                    if (names.size() > 0) {
                        name = names.get(0);
                    }
                }
            }
            profile.setName(name);
            for (Object key : dictionary.keySet()) {
                if (key.equals("ID")) {
                    continue;
                }
                if (key.equals("name") || key.equals("names")) {
                    continue;
                }
                profile.setProperty((String) key, dictionary.get(key));
            }
            // sign profile
            profile.sign(privateKey);
        } else {
            String signature = (String) dictionary.get("signature");
            if (signature == null) {
                profile = new Profile(identifier, profile_data, null);
                // sign profile
                profile.sign(privateKey);
            } else {
                profile = new Profile(identifier, profile_data, Base64.decode(signature));
                // verify
                profile.verify(privateKey.getPublicKey());
            }
        }
        return profile;
    }

    @SuppressWarnings("unchecked")
    private void loadBuiltInAccount(String filename) throws IOException, ClassNotFoundException {
        // load from resource directory
        Resource file = new Resource();
        if (file.load(filename) <= 0) {
            throw new IOException("failed to load built-in account file: " + filename);
        }
        String jsonString = new String(file.getData(), Charset.forName("UTF-8"));
        Map<String, Object> dict = (Map<String, Object>) JSON.decode(jsonString);
        Log.info(filename + ":" + dict);
        // ID
        ID identifier = ID.getInstance(dict.get("ID"));
        assert identifier != null;
        // meta
        Meta meta = Meta.getInstance(dict.get("meta"));
        assert meta != null;
        if (meta.matches(identifier)) {
            metaMap.put(identifier.address, meta);
        } else {
            throw new IllegalArgumentException("meta not match ID:" + identifier + ", " + meta);
        }
        // private key
        PrivateKey privateKey = PrivateKeyImpl.getInstance(dict.get("privateKey"));
        assert privateKey != null;
        if (meta.getKey().matches(privateKey)) {
            // TODO: store private key into keychain
            privateKeyMap.put(identifier.address, privateKey);
        } else {
            throw new IllegalArgumentException("private key not match meta public key:" + privateKey);
        }
        // profile
        Map profile = (Map) dict.get("profile");
        if (profile != null) {
            profileMap.put(identifier.address, getProfile(profile, identifier, privateKey));
        }
    }

    //---- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        return metaMap.get(identifier.address);
    }

    @Override
    public Profile getProfile(ID identifier) {
        return profileMap.get(identifier.address);
    }

    //---- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        return null;
    }

    @Override
    public SignKey getPrivateKeyForSignature(ID user) {
        return privateKeyMap.get(user.address);
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        return null;
    }

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        return null;
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        List<DecryptKey> keys = new ArrayList<>();
        PrivateKey key = privateKeyMap.get(user.address);
        if (key != null) {
            // TODO: support profile.key
            assert key instanceof DecryptKey;
            keys.add((DecryptKey) key);
        }
        return keys;
    }

    static {
        // FIXME: test

        // load data
        Immortals immortals = Immortals.getInstance();
        try {
            immortals.loadBuiltInAccount("/mkm_hulk.js");
            immortals.loadBuiltInAccount("/mkm_moki.js");
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
