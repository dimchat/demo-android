package chat.dim.sechat.account;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.SignKey;
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
        // upload to server
        Meta meta = facebook.getMeta(identifier);
        Messenger messenger = Messenger.getInstance();
        messenger.postProfile(profile, meta);
        // broadcast to all contacts
        messenger.broadcastProfile(profile);
    }
}
