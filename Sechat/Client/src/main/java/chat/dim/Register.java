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
package chat.dim;

import java.util.Random;

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.crypto.AsymmetricKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.PublicKey;
import chat.dim.database.PrivateKeyTable;
import chat.dim.mkm.BaseBulletin;
import chat.dim.mkm.BaseVisa;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.Visa;
import chat.dim.utils.Log;

/**
 *  This is for generating user account, or creating group
 */
public final class Register {

    private PrivateKey privateKey = null; // user private key

    public Register() {
    }

    /**
     *  Generate user account
     *
     * @param name - nickname
     * @param avatar - photo URL
     * @return User object
     */
    public User createUser(String name, String avatar) {
        //
        //  Step 1. generate private key (with asymmetric algorithm)
        //
        privateKey = PrivateKey.generate(PrivateKey.ECC);
        //
        //  Step 2. generate meta with private key (and meta seed)
        //
        Meta meta = Meta.generate(MetaType.ETH.value, privateKey, null);
        //
        //  Step 3. generate ID with meta
        //
        ID identifier = meta.generateID(NetworkType.MAIN.value, null);
        //
        //  Step 4. generate visa with ID and sign with private key
        //
        PrivateKey priKey = PrivateKey.generate(AsymmetricKey.RSA);
        PublicKey msgKey = priKey.getPublicKey();
        Visa visa = createUserDocument(identifier, name, avatar, (EncryptKey) msgKey);
        // 5. save private key, meta & visa in local storage
        //    don't forget to upload them onto the DIM station
        Facebook facebook = Messenger.getInstance().getFacebook();
        facebook.saveMeta(meta, identifier);
        facebook.savePrivateKey(privateKey, identifier, PrivateKeyTable.META);
        facebook.savePrivateKey(priKey, identifier, PrivateKeyTable.VISA);
        facebook.saveDocument(visa);
        // 6. create user
        return facebook.getUser(identifier);
    }

    /**
     *  Generate group account
     *
     * @param founder - group founder
     * @param name - group name
     * @return Group object
     */
    public Group createGroup(ID founder, String name) {
        Random random = new Random();
        long r = random.nextInt(999990000) + 10000; // 10,000 ~ 999,999,999
        return createGroup(founder, name, "Group-" + r);
    }
    public Group createGroup(ID founder, String name, String seed) {
        Facebook facebook = Messenger.getInstance().getFacebook();
        // 1. get private key
        privateKey = (PrivateKey) facebook.getPrivateKeyForVisaSignature(founder);
        // 2. generate meta
        Meta meta = Meta.generate(MetaType.DEFAULT.value, privateKey, seed);
        // 3. generate ID
        ID identifier = meta.generateID(NetworkType.POLYLOGUE.value, null);
        // 4. generate document
        Bulletin bulletin = createGroupDocument(identifier, name);
        // 5. save meta & bulletin in local storage
        //    don't forget to upload them onto the DIM station
        facebook.saveMeta(meta, identifier);
        facebook.saveDocument(bulletin);
        // 6. add founder as first member
        facebook.addMember(founder, identifier);
        // 7. create group
        return facebook.getGroup(identifier);
    }

    public Visa createUserDocument(ID identifier, String name, String avatarUrl, EncryptKey key) {
        assert identifier.isUser() : "ID error";
        assert privateKey != null : "private key not found";
        BaseVisa visa = new BaseVisa(identifier);
        visa.setName(name);
        visa.setAvatar(avatarUrl);
        visa.setKey(key);
        visa.sign(privateKey);
        return visa;
    }
    public Bulletin createGroupDocument(ID identifier, String name) {
        assert identifier != null : "ID error";
        assert privateKey != null : "private key not found";
        BaseBulletin bulletin = new BaseBulletin(identifier);
        bulletin.setName(name);
        bulletin.sign(privateKey);
        return bulletin;
    }

    // upload meta & document for ID
    public boolean upload(ID identifier, Meta meta, Document doc) {
        assert identifier != null : "ID error";
        assert identifier.equals(doc.getIdentifier()) : "document ID not match";
        Messenger messenger = Messenger.getInstance();
        return messenger.postDocument(doc, meta);
    }

    /**
     *  Test case
     *
     * @param args - command arguments
     */
    public static void main(String[] args) {
        // 1. create user
        Register userRegister = new Register();
        User user = userRegister.createUser("Albert Moky", null);
        Log.info("user: " + user);
        //userRegister.upload(user.identifier, user.getMeta(), user.getDocument());

        // 2. create group
        Register groupRegister = new Register();
        Group group = groupRegister.createGroup(user.identifier, "DIM Group");
        Log.info("group: " + group);
        //groupRegister.upload(group.identifier, group.getMeta(), group.getDocument());
    }
}
