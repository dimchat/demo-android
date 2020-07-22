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
import java.util.Random;

import chat.dim.Group;
import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.PrivateKey;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.NetworkType;
import chat.dim.utils.Log;

/**
 *  This is for generating user account, or creating group
 */
public class Register {

    private final NetworkType network; // user type (Main: 0x08)

    private PrivateKey privateKey = null; // user private key

    public Register() {
        this(NetworkType.Main);
    }

    public Register(NetworkType type) {
        super();
        network = type;
    }

    /**
     *  Generate user account
     *
     * @param name - nickname
     * @param avatar - photo URL
     * @return User object
     */
    public User createUser(String name, String avatar) {
        Facebook facebook = Facebook.getInstance();
        // 1. generate private key
        PrivateKey key = generatePrivateKey();
        // 2. generate meta
        Meta meta = generateMeta("sechat");
        // 3. generate ID
        ID identifier = generateID(meta, NetworkType.Main);
        // 4. generate profile
        Profile profile = createProfile(identifier, name, avatar);
        // 5. save private key, meta & profile in local storage
        //    don't forget to upload them onto the DIM station
        facebook.saveMeta(meta, identifier);
        facebook.savePrivateKey(key, identifier);
        facebook.saveProfile(profile);
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
        Facebook facebook = Facebook.getInstance();
        // 1. get private key
        privateKey = (PrivateKey) facebook.getPrivateKeyForSignature(founder);
        // 2. generate meta
        Meta meta = generateMeta(seed);
        // 3. generate ID
        ID identifier = generateID(meta, NetworkType.Polylogue);
        // 4. generate profile
        Profile profile = createProfile(identifier, name);
        // 5. save meta & profile in local storage
        //    don't forget to upload them onto the DIM station
        facebook.saveMeta(meta, identifier);
        facebook.saveProfile(profile);
        // 6. add founder as first member
        facebook.addMember(founder, identifier);
        // 7. create group
        return facebook.getGroup(identifier);
    }

    //
    //  Step 1. generate private key (with asymmetric algorithm)
    //
    public PrivateKey generatePrivateKey() {
        return generatePrivateKey(PrivateKey.RSA);
    }
    public PrivateKey generatePrivateKey(String algorithm) {
        privateKey = null;
        try {
            privateKey = PrivateKey.generate(algorithm);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
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
        assert privateKey != null : "private key not found";
        try {
            return Meta.generate(MetaType.Default, privateKey, seed);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    //
    //  Step 3. generate ID with meta (and network type)
    //
    public ID generateID(Meta meta) {
        return generateID(meta, network);
    }
    public ID generateID(Meta meta, NetworkType type) {
        assert meta != null : "meta not found";
        return meta.generateID(type);
    }

    //
    //  Step 4. create profile with ID and sign with private key
    //
    public Profile createProfile(ID identifier, String name) {
        return createProfile(identifier, name, null);
    }
    public Profile createProfile(ID identifier, String name, String avatarUrl) {
        assert identifier != null : "ID error";
        assert privateKey != null : "profile not found";
        Profile profile;
        if (identifier.isUser()) {
            profile = new UserProfile(identifier);
        } else {
            profile = new Profile(identifier);
        }
        profile.setName(name);
        if (avatarUrl != null) {
            assert profile instanceof UserProfile : "profile error: " + profile;
            ((UserProfile) profile).setAvatar(avatarUrl);
        }
        profile.sign(privateKey);
        return profile;
    }
    public Profile createProfile(ID identifier, Map<String, Object> properties) {
        assert identifier != null : "ID error";
        assert privateKey != null : "private key not found";
        Profile profile;
        if (identifier.isUser()) {
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
    //  Step 5. upload meta & profile for ID
    //
    public boolean upload(ID identifier, Meta meta, Profile profile) {
        assert identifier != null : "ID error";
        assert identifier.equals(profile.getIdentifier()) : "profile ID not match";
        Messenger messenger = Messenger.getInstance();
        return messenger.postProfile(profile, meta);
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
        //userRegister.upload(user.identifier, user.getMeta(), user.getProfile());

        // 2. create group
        Register groupRegister = new Register(NetworkType.Polylogue);
        Group group = groupRegister.createGroup(user.identifier, "DIM Group");
        Log.info("group: " + group);
        //groupRegister.upload(group.identifier, group.getMeta(), group.getProfile());
    }
}
