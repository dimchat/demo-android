package chat.dim.sechat.account;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.SignKey;
import chat.dim.format.JSON;
import chat.dim.model.Messenger;
import chat.dim.sechat.model.UserViewModel;

public class AccountViewModel extends UserViewModel {

    public ID getIdentifier() {
        if (identifier == null) {
            User user = getCurrentUser();
            if (user == null) {
                throw new NullPointerException("current user not found");
            }
            identifier = user.identifier;
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
}
