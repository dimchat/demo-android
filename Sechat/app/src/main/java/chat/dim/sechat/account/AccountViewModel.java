package chat.dim.sechat.account;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.AsymmetricKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.format.JSON;
import chat.dim.model.Messenger;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.NetworkType;
import chat.dim.sechat.model.UserViewModel;

public class AccountViewModel extends UserViewModel {

    public ID getIdentifier() {
        if (identifier == null) {
            User user = getCurrentUser();
            if (user != null) {
                identifier = user.identifier;
            }
        }
        return identifier;
    }

    void updateProfile(Profile profile) {
        ID identifier = getIdentifier();
        if (identifier == null || !identifier.equals(profile.getIdentifier())) {
            return;
        }
        // get private key to sign the profile
        SignKey privateKey = facebook.getPrivateKeyForSignature(identifier);
        if (privateKey == null) {
            throw new NullPointerException("failed to get private key: " + identifier);
        }
        profile.sign(privateKey);
        // save signed profile
        if (!facebook.saveProfile(profile)) {
            return;
        }
        Messenger messenger = Messenger.getInstance();
        // upload to server
        Meta meta = facebook.getMeta(identifier);
        messenger.postProfile(profile, meta);
        // broadcast to all contacts
        messenger.broadcastProfile(profile);
    }

    String serializePrivateInfo() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        Meta meta = facebook.getMeta(identifier);
        SignKey privateKey = facebook.getPrivateKeyForSignature(identifier);
        if (meta == null || privateKey == null) {
            return null;
        }

        String pem = (String) privateKey.get("data");
        if (pem == null) {
            return null;
        }
        int begin = pem.indexOf("-----BEGIN PUBLIC KEY-----");
        if (begin >= 0) {
            int end = pem.indexOf("-----END PUBLIC KEY-----", begin + "-----BEGIN PUBLIC KEY-----".length());
            if (end > 0) {
                String tail = pem.substring(end + "-----END PUBLIC KEY-----".length());
                if (begin == 0) {
                    pem = tail;
                } else {
                    pem = pem.substring(0, begin) + tail;
                }
                pem = pem.trim();
            }
        }

        Map<String, Object> info = new HashMap<>();

        // meta version
        info.put("version", meta.getVersion());

        // key data
        info.put("data", pem);

        // key algorithm
        String algorithm = (String) privateKey.get("algorithm");
        if (algorithm != null && algorithm.length() > 0) {
            info.put("algorithm", algorithm);
        }

        // ID.seed
        String seed = identifier.name;
        if (seed != null && seed.length() > 0) {
            info.put("username", seed);
        }

        // profile.name
        String name = facebook.getNickname(identifier);
        if (name != null && name.length() > 0) {
            info.put("nickname", name);
        }

        byte[] data = JSON.encode(info);
        return new String(data, Charset.forName("UTF-8"));
    }

    public ID savePrivateInfo(String json) {
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        //noinspection unchecked
        Map<String, Object> info = (Map<String, Object>) JSON.decode(data);
        if (info == null) {
            return null;
        }

        Object value;

        // data of private key
        String base64 = (String) info.get("data");
        if (base64 == null) {
            return null;
        }

        // algorithm of private key
        String algorithm = (String) info.get("algorithm");
        if (algorithm == null) {
            algorithm = AsymmetricKey.RSA;
        }

        // meta.version
        int version = 0;
        value = info.get("version");
        if (value instanceof Number) {
            version = ((Number) value).intValue();
        }
        if (version == 0) {
            version = MetaType.Default.value;
        }

        // ID.type
        byte network = 0;
        value = info.get("type");
        if (value == null) {
            value = info.get("network");
        }
        if (value instanceof Number) {
            network = ((Number) value).byteValue();
        }
        if (network == 0) {
            network = NetworkType.Main.value;
        }

        // ID.seed
        String seed = (String) info.get("seed");
        if (seed == null) {
            seed = (String) info.get("username");
            if (seed == null) {
                seed = "dim";
            }
        }

        // profile.name
        String nickname = (String) info.get("nickname");

        return savePrivateInfo(base64, algorithm, version, network, seed, nickname);
    }

    private ID savePrivateInfo(String keyData, String algorithm, int metaVersion, byte network, String seed, String nickname) {
        // generate private key
        Map<String, Object> keyInfo = new HashMap<>();
        keyInfo.put("algorithm", algorithm);
        keyInfo.put("data", keyData);
        PrivateKey privateKey = null;
        try {
            privateKey = PrivateKey.getInstance(keyInfo);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (privateKey == null) {
            return null;
        }

        // generate meta
        Meta meta = null;
        try {
            meta = Meta.generate(metaVersion, privateKey, seed);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (meta == null) {
            return null;
        }

        // generate ID
        ID identifier = meta.generateID(network);
        if (identifier == null) {
            return null;
        }

        // save private key with user ID
        if (!facebook.savePrivateKey(privateKey, identifier)) {
            return null;
        }

        // save meta with user ID
        if (!facebook.saveMeta(meta, identifier)) {
            return null;
        }

        // generate profile
        if (nickname != null && nickname.length() > 0) {
            Profile profile = new Profile(identifier);
            profile.setName(nickname);
            if (profile.sign(privateKey) == null) {
                return null;
            }
            if (!facebook.saveProfile(profile)) {
                return null;
            }
        }

        // OK
        return identifier;
    }
}
