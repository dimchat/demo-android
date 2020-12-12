package chat.dim.sechat.account;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.User;
import chat.dim.crypto.AsymmetricKey;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.format.Hex;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.BTCMeta;
import chat.dim.mkm.BaseVisa;
import chat.dim.mkm.DefaultMeta;
import chat.dim.mkm.ETHMeta;
import chat.dim.model.Messenger;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
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

    public List<ID> getContacts() {
        User user = facebook.getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.getContacts();
    }

    public void updateProfile(Document profile) {
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
        if (!facebook.saveDocument(profile)) {
            return;
        }
        Messenger messenger = Messenger.getInstance();
        // upload to server
        Meta meta = facebook.getMeta(identifier);
        messenger.postProfile(profile, meta);
        // broadcast to all contacts
        messenger.broadcastProfile(profile);
    }

    public String serializePrivateInfo() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        Meta meta = facebook.getMeta(identifier);
        SignKey privateKey = facebook.getPrivateKeyForSignature(identifier);
        if (meta == null || privateKey == null) {
            return null;
        }

        String pem;
        if (privateKey.getAlgorithm().equals(AsymmetricKey.ECC)) {
            byte[] data = privateKey.getData();
            if (data == null) {
                return null;
            }
            pem = Hex.encode(data);
        } else {
            pem = (String) privateKey.get("data");
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
        }

        Map<String, Object> info = new HashMap<>();

        // meta version
        info.put("version", meta.getType());

        // key data
        info.put("data", pem);

        // key algorithm
        String algorithm = (String) privateKey.get("algorithm");
        if (algorithm != null && algorithm.length() > 0) {
            info.put("algorithm", algorithm);
        }

        // ID.seed
        String seed = identifier.getName();
        if (seed != null && seed.length() > 0) {
            info.put("seed", seed);
        }

        // nickname
        String nickname = facebook.getNickname(identifier);
        if (nickname != null && nickname.length() > 0) {
            info.put("nickname", nickname);
        }

        byte[] data = JSON.encode(info);
        return UTF8.decode(data);
    }

    @SuppressWarnings("unchecked")
    public ID savePrivateInfo(String json) {
        Map<String, Object> info = null;
        if (json.length() == 64) {
            // ECC private key
            info = new HashMap<>();
            info.put("data", json);
            info.put("algorithm", AsymmetricKey.ECC);
            info.put("version", MetaType.ETH.value);
        } else if (json.startsWith("{")) {
            // dictionary
            byte[] data = UTF8.encode(json);
            info = (Map<String, Object>) JSON.decode(data);
        }
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
        PrivateKey privateKey = PrivateKey.parse(keyInfo);
        if (privateKey == null) {
            return null;
        }

        // generate meta
        Meta meta = Meta.generate(metaVersion, privateKey, seed);

        // generate ID
        ID identifier;
        if (meta instanceof DefaultMeta) {
            identifier = ((DefaultMeta) meta).generateID(network);
        } else if (meta instanceof BTCMeta) {
            identifier = ((BTCMeta) meta).generateID();
        } else {
            identifier = ((ETHMeta) meta).generateID();
        }
        if (identifier == null) {
            return null;
        }

        // save private key with user ID
        if (!facebook.savePrivateKey(privateKey, identifier, "M")) {
            return null;
        }

        EncryptKey msgKey;
        if (privateKey instanceof DecryptKey) {
            msgKey = null;
        } else {
            PrivateKey rsaKey = PrivateKey.generate(AsymmetricKey.RSA);
            if (!facebook.savePrivateKey(rsaKey, identifier, "P")) {
                return null;
            }
            msgKey = (EncryptKey) rsaKey.getPublicKey();
        }

        // save meta with user ID
        if (!facebook.saveMeta(meta, identifier)) {
            return null;
        }

        // generate profile
        if (nickname != null || msgKey != null) {
            BaseVisa profile = new BaseVisa(identifier);
            if (nickname != null && nickname.length() > 0) {
                profile.setName(nickname);
            }
            if (msgKey != null) {
                profile.setKey(msgKey);
            }
            if (profile.sign(privateKey) == null) {
                return null;
            }
            if (!facebook.saveDocument(profile)) {
                return null;
            }
        }

        // OK
        return identifier;
    }

    public ID removeCurrentUser() {
        ID current = identifier;
        if (facebook.removeUser(current)) {
            identifier = null;
        }
        return current;
    }
}
