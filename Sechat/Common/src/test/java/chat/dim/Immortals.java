/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.crypto.VerifyKey;
import chat.dim.filesys.Paths;
import chat.dim.filesys.Resource;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.BaseUser;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.Visa;

/**
 *  Built-in accounts (for test)
 *
 *      1. Immortal Hulk - hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj
 *      2. Monkey King   - moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk
 */
public final class Immortals implements User.DataSource {

    // Immortal Hulk (195-183-9394)
    public static final String HULK = "hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj";
    // Monkey King (184-083-9527)
    public static final String MOKI = "moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk";

    // memory caches
    private Map<String, ID>     idMap         = new HashMap<>();
    private Map<ID, PrivateKey> privateKeyMap = new HashMap<>();
    private Map<ID, Meta>       metaMap       = new HashMap<>();
    private Map<ID, Visa>       visaMap       = new HashMap<>();
    private Map<ID, User>       userMap       = new HashMap<>();

    public Immortals() {
        super();
        try {
            // load built-in users
            loadBuiltInAccount(ID.parse(MOKI));
            loadBuiltInAccount(ID.parse(HULK));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Resource Loader for built-in accounts
     */
    private static String root = File.separator + "mkm";

    private static Map loadJSON(String filename) throws IOException {
        String path = Paths.append(root, filename);
        Resource resource = new Resource();
        resource.read(path);
        byte[] data = resource.getData();
        return (Map) JSON.decode(UTF8.decode(data));
    }

    private void loadBuiltInAccount(ID identifier) throws IOException {
        boolean OK = cache(identifier);
        assert OK : "ID error: " + identifier;
        // load meta for ID
        Meta meta = loadMeta(identifier.getName() + "_meta.js");
        OK = cache(meta, identifier);
        assert OK : "meta error: " + meta;
        // load private key for ID
        PrivateKey key = loadPrivateKey(identifier.getName() + "_secret.js");
        OK = cache(key, identifier);
        assert OK : "private key error: " + key;
        // load visa for ID
        Visa visa = loadVisa(identifier.getName() + "_profile.js");
        OK = cache(visa, identifier);
        assert OK : "visa error: " + visa;
    }

    @SuppressWarnings("unchecked")
    private Meta loadMeta(String filename) throws IOException {
        Map dict = loadJSON(filename);
        return Meta.parse(dict);
    }

    @SuppressWarnings("unchecked")
    private PrivateKey loadPrivateKey(String filename) throws IOException {
        Map dict = loadJSON(filename);
        return PrivateKey.parse(dict);
    }

    @SuppressWarnings("unchecked")
    private Visa loadVisa(String filename) throws IOException {
        Map dict = loadJSON(filename);
        Visa visa = (Visa) Document.parse(dict);
        assert visa != null : "failed to load visa: " + filename + ", " + dict;
        // copy 'name'
        Object name = dict.get("name");
        if (name == null) {
            Object names = dict.get("names");
            if (names instanceof List) {
                List array = (List) names;
                if (array.size() > 0) {
                    visa.setProperty("name", array.get(0));
                }
            }
        } else {
            visa.setProperty("name", name);
        }
        // copy 'avatar'
        Object avatar = dict.get("avatar");
        if (avatar == null) {
            Object photos = dict.get("photos");
            if (photos instanceof List) {
                List array = (List) photos;
                if (array.size() > 0) {
                    visa.setProperty("avatar", array.get(0));
                }
            }
        } else {
            visa.setProperty("avatar", avatar);
        }
        // sign
        byte[] s = sign(visa);
        assert s != null : "failed to sign visa: " + visa;
        return visa;
    }

    private byte[] sign(Visa visa) {
        ID identifier = getID(visa.getIdentifier());
        SignKey key = getPrivateKeyForVisaSignature(identifier);
        assert key != null : "failed to get private key for signature: " + identifier;
        return visa.sign(key);
    }

    //-------- cache

    private boolean cache(ID identifier) {
        idMap.put(identifier.toString(), identifier);
        return true;
    }

    private boolean cache(Meta meta, ID identifier) {
        assert Meta.matches(identifier, meta) : "meta not match: " + identifier + ", " + meta;
        metaMap.put(identifier, meta);
        return true;
    }

    private boolean cache(PrivateKey key, ID identifier) {
        privateKeyMap.put(identifier, key);
        return true;
    }

    private boolean cache(Visa visa, ID identifier) {
        assert visa.isValid() : "visa not valid: " + visa;
        assert identifier.equals(visa.getIdentifier()) : "visa not match: " + identifier + ", " + visa;
        visaMap.put(identifier, visa);
        return true;
    }

    private boolean cache(User user) {
        if (user.getDataSource() == null) {
            user.setDataSource(this);
        }
        userMap.put(user.getIdentifier(), user);
        return true;
    }

    //-------- create

    public ID getID(Object string) {
        if (string == null) {
            return null;
        } else if (string instanceof ID) {
            return (ID) string;
        }
        assert string instanceof String : "ID string error: " + string;
        return idMap.get(string);
    }

    public User getUser(ID identifier) {
        User user = userMap.get(identifier);
        if (user == null) {
            // only create exists account
            if (idMap.containsValue(identifier)) {
                user = new BaseUser(identifier);
                boolean OK = cache(user);
                assert OK : "failed to cache user: " + user;
            }
        }
        return user;
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        return metaMap.get(identifier);
    }

    @Override
    public Document getDocument(ID identifier, String type) {
        return visaMap.get(identifier);
    }

    //-------- UserDataSource

    @Override
    public List<ID> getContacts(ID user) {
        if (!idMap.containsValue(user)) {
            return null;
        }
        List<ID> contacts = new ArrayList<>();
        for (ID value : idMap.values()) {
            if (value.equals(user)) {
                continue;
            }
            contacts.add(value);
        }
        return contacts;
    }

    private EncryptKey getVisaKey(ID user) {
        Document doc = getDocument(user, Document.VISA);
        if (doc instanceof Visa) {
            Visa visa = (Visa) doc;
            if (visa.isValid()) {
                return visa.getKey();
            }
        }
        return null;
    }
    private VerifyKey getMetaKey(ID user) {
        Meta meta = getMeta(user);
        if (meta == null) {
            //throw new NullPointerException("failed to get meta for ID: " + user);
            return null;
        }
        return meta.getKey();
    }

    @Override
    public EncryptKey getPublicKeyForEncryption(ID user) {
        // 1. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey != null) {
            // if visa.key exists, use it for encryption
            return visaKey;
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey instanceof EncryptKey) {
            // if profile.key not exists and meta.key is encrypt key,
            // use it for encryption
            return (EncryptKey) metaKey;
        }
        //throw new NullPointerException("failed to get encrypt key for user: " + user);
        return null;
    }

    @Override
    public List<VerifyKey> getPublicKeysForVerification(ID user) {
        List<VerifyKey> keys = new ArrayList<>();
        // 1. get key from visa
        EncryptKey visaKey = getVisaKey(user);
        if (visaKey instanceof VerifyKey) {
            // the sender may use communication key to sign message.data,
            // so try to verify it with visa.key here
            keys.add((VerifyKey) visaKey);
        }
        // 2. get key from meta
        VerifyKey metaKey = getMetaKey(user);
        if (metaKey != null) {
            // the sender may use identity key to sign message.data,
            // try to verify it with meta.key
            keys.add(metaKey);
        }
        assert keys.size() > 0 : "failed to get verify key for user: " + user;
        return keys;
    }

    @Override
    public List<DecryptKey> getPrivateKeysForDecryption(ID user) {
        PrivateKey key = privateKeyMap.get(user);
        if (key instanceof DecryptKey) {
            List<DecryptKey> array = new ArrayList<>();
            array.add((DecryptKey) key);
            return array;
        }
        return null;
    }

    @Override
    public SignKey getPrivateKeyForSignature(ID user) {
        return privateKeyMap.get(user);
    }

    @Override
    public SignKey getPrivateKeyForVisaSignature(ID user) {
        return privateKeyMap.get(user);
    }
}
