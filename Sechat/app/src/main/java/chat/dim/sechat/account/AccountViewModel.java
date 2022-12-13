package chat.dim.sechat.account;

import java.util.HashMap;
import java.util.Map;

import chat.dim.client.Messenger;
import chat.dim.crypto.AsymmetricKey;
import chat.dim.crypto.DecryptKey;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.PrivateKey;
import chat.dim.crypto.SignKey;
import chat.dim.format.Hex;
import chat.dim.format.JSON;
import chat.dim.mkm.BaseVisa;
import chat.dim.mkm.User;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaType;
import chat.dim.protocol.Visa;
import chat.dim.sechat.model.UserViewModel;

public class AccountViewModel extends UserViewModel {

    @Override
    public User getUser() {
        User user = super.getUser();
        if (user == null) {
            user = getFacebook().getCurrentUser();
            if (user != null) {
                setEntity(user);
            }
        }
        return user;
    }

    public void updateVisa(Visa visa) {
        ID identifier = getIdentifier();
        if (identifier == null || !identifier.equals(visa.getIdentifier())) {
            return;
        }
        // get private key to sign the visa document
        SignKey sKey = getFacebook().getPrivateKeyForVisaSignature(identifier);
        if (sKey == null) {
            throw new NullPointerException("failed to get private key: " + identifier);
        }
        visa.sign(sKey);
        // save signed visa document
        if (!getFacebook().saveDocument(visa)) {
            return;
        }
        Messenger messenger = Messenger.getInstance();
        // upload to server
        Meta meta = getFacebook().getMeta(identifier);
        messenger.postDocument(visa, meta);
        // broadcast to all contacts
        messenger.broadcastVisa(visa);
    }

    public String serializePrivateInfo() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        Meta meta = getFacebook().getMeta(identifier);
        SignKey sKey = getFacebook().getPrivateKeyForVisaSignature(identifier);
        if (meta == null || sKey == null) {
            return null;
        }

        String pem;
        if (sKey.getAlgorithm().equals(AsymmetricKey.ECC)) {
            byte[] data = sKey.getData();
            if (data == null) {
                return null;
            }
            pem = Hex.encode(data);
        } else {
            pem = (String) sKey.get("data");
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
        info.put("type", meta.getType());

        // key data
        info.put("data", pem);

        // key algorithm
        String algorithm = (String) sKey.get("algorithm");
        if (algorithm != null && algorithm.length() > 0) {
            info.put("algorithm", algorithm);
        }

        // ID.seed
        String seed = identifier.getName();
        if (seed != null && seed.length() > 0) {
            info.put("seed", seed);
        }

        // nickname
        Document doc = getFacebook().getDocument(identifier, "*");
        if (doc != null) {
            info.put("nickname", doc.getName());
        }

        return JSON.encode(info);
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
            info = (Map<String, Object>) JSON.decode(json);
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
        if (value == null) {
            value = info.get("type");
        }
        if (value instanceof Number) {
            version = ((Number) value).intValue();
        }
        if (version == 0) {
            version = MetaType.DEFAULT.value;
        }

        // ID.type
        int network = 0;
        value = info.get("type");
        if (value == null) {
            value = info.get("network");
        }
        if (value instanceof Number) {
            network = ((Number) value).intValue();
        }

        // ID.seed
        String seed = (String) info.get("seed");
        if (seed == null) {
            seed = (String) info.get("username");
        }

        // visa.name
        String nickname = (String) info.get("nickname");

        return savePrivateInfo(base64, algorithm, version, network, seed, nickname);
    }

    private ID savePrivateInfo(String keyData, String algorithm, int metaVersion, int network, String seed, String nickname) {
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
        ID identifier = ID.generate(meta, network, null);

        // save private key with user ID
        if (!getFacebook().savePrivateKey(privateKey, identifier, "M")) {
            return null;
        }

        EncryptKey msgKey;
        if (privateKey instanceof DecryptKey) {
            msgKey = null;
        } else {
            PrivateKey rsaKey = PrivateKey.generate(AsymmetricKey.RSA);
            if (!getFacebook().savePrivateKey(rsaKey, identifier, "P")) {
                return null;
            }
            msgKey = (EncryptKey) rsaKey.getPublicKey();
        }

        // save meta with user ID
        if (!getFacebook().saveMeta(meta, identifier)) {
            return null;
        }

        // generate visa
        if (nickname != null || msgKey != null) {
            BaseVisa visa = new BaseVisa(identifier);
            if (nickname != null && nickname.length() > 0) {
                visa.setName(nickname);
            }
            if (msgKey != null) {
                visa.setKey(msgKey);
            }
            if (visa.sign(privateKey) == null) {
                return null;
            }
            if (!getFacebook().saveDocument(visa)) {
                return null;
            }
        }

        // OK
        return identifier;
    }

    public ID removeCurrentUser() {
        ID current = getIdentifier();
        if (getFacebook().removeUser(current)) {
            setEntity(null);
        }
        return current;
    }
}
