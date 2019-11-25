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
package chat.dim.extension;

import java.util.Map;

import chat.dim.Entity;
import chat.dim.Group;
import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.PrivateKey;
import chat.dim.impl.PrivateKeyImpl;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.plugins.UserProfile;
import chat.dim.protocol.Command;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.ProfileCommand;
import chat.dim.utils.Log;

public class Register {

    private NetworkType network;

    public PrivateKey privateKey = null;
    public Meta meta = null;
    public ID identifier = null;
    public Profile profile = null;
    public Entity account = null;

    public Register() {
        this(NetworkType.Main);
    }

    public Register(NetworkType type) {
        super();
        network = type;
    }

    private Facebook facebook = Facebook.getInstance();
    private Messenger messenger = Messenger.getInstance();

    /**
     *  Generate user account
     *
     * @param name - nickname
     * @param avatar - photo URL
     * @return User object
     */
    public User createUser(String name, String avatar) {
        // 1. generate private key
        generatePrivateKey();
        // 2. generate meta
        Meta meta = generateMeta("user");
        // 3. generate ID
        ID identifier = generateID(NetworkType.Main);
        // 4. generate profile
        Profile profile = createProfile(name, avatar);
        // 5. save private key, meta & profile in local storage
        //    don't forget to upload them onto the DIM station
        facebook.savePrivateKey(privateKey, identifier);
        facebook.saveMeta(meta, identifier);
        facebook.saveProfile(profile);
        // 6. create user
        User user = facebook.getUser(identifier);
        account = user;
        return user;
    }

    /**
     *  Generate group account
     *
     * @param name - group name
     * @param founder - group founder
     * @return Group object
     */
    public Group createGroup(String name, User founder) {
        return createGroup(name, founder.identifier);
    }
    public Group createGroup(String name, ID founder) {
        // 1. get private key
        privateKey = (PrivateKey) facebook.getPrivateKeyForSignature(founder);
        // 2. generate meta
        Meta meta = generateMeta("group");
        // 3. generate ID
        ID identifier = generateID(NetworkType.Polylogue);
        // 4. generate profile
        Profile profile = createProfile(name);
        // 5. save meta & profile in local storage
        //    don't forget to upload them onto the DIM station
        facebook.saveMeta(meta, identifier);
        facebook.saveProfile(profile);
        // 6. create user
        Group group = facebook.getGroup(identifier);
        account = group;
        return group;
    }

    //
    //  Step 1. generate private key (with asymmetric algorithm)
    //
    public PrivateKey generatePrivateKey() {
        return generatePrivateKey(PrivateKey.RSA);
    }
    public PrivateKey generatePrivateKey(String algorithm) {
        try {
            privateKey = PrivateKeyImpl.generate(algorithm);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            privateKey = null;
        }
        return privateKey;
    }

    //
    //  Step 2. generate meta with private key (and meta seed)
    //
    public Meta generateMeta() {
        return generateMeta("anonymous");
    }
    public Meta generateMeta(String seed) {
        assert privateKey != null;
        try {
            meta = Meta.generate(MetaType.Default, privateKey, seed);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            meta = null;
        }
        return meta;
    }

    //
    //  Step 3. generate ID with meta (and network type)
    //
    public ID generateID() {
        return generateID(network);
    }
    public ID generateID(NetworkType type) {
        assert meta != null;
        identifier = meta.generateID(type);
        network = type;
        return identifier;
    }

    //
    //  Step 4. create profile with ID and private key
    //
    public Profile createProfile(String name) {
        return createProfile(name, null);
    }
    public Profile createProfile(String name, String avatarUrl) {
        assert identifier != null;
        assert privateKey != null;
        Profile profile;
        if (identifier.getType().isUser()) {
            profile = new UserProfile(identifier);
        } else {
            profile = new Profile(identifier);
        }
        profile.setName(name);
        if (avatarUrl != null) {
            profile.setProperty("avatar", avatarUrl);
        }
        profile.sign(privateKey);
        return profile;
    }
    public Profile createProfile(Map<String, Object> properties) {
        assert identifier != null;
        assert privateKey != null;
        Profile profile;
        if (identifier.getType().isUser()) {
            profile = new UserProfile(identifier);
        } else {
            profile = new Profile(identifier);
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()){
            profile.setProperty(entry.getKey(), entry.getValue());
        }
        profile.sign(privateKey);
        return profile;
    }

    //
    //  Step 5. upload meta & profile
    //
    public boolean upload() {
        assert identifier != null;
        Command cmd;
        if (profile == null) {
            assert meta.matches(identifier);
            cmd = new MetaCommand(identifier, meta);
        } else {
            assert profile.getIdentifier().equals(identifier);
            assert profile.isValid();
            cmd = new ProfileCommand(identifier, meta, profile);
        }
        return messenger.sendCommand(cmd);
    }

    /**
     *  Test case
     *
     * @param args - command arguments
     */
    public static void main(String args[]) {
        // 1. create user
        Register userRegister = new Register();
        User user = userRegister.createUser("moky", null);
        Log.info("user: " + user);
        //userRegister.upload();

        // 2. create group
        Register groupRegister = new Register(NetworkType.Polylogue);
        Group group = groupRegister.createGroup("DIM Group", user);
        Log.info("group: " + group);
        //groupRegister.upload();
    }
}
