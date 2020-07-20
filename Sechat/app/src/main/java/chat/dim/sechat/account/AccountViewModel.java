package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModel;
import android.net.Uri;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.SignKey;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;

public class AccountViewModel extends ViewModel {

    private Facebook facebook = Facebook.getInstance();

    private User currentUser = null;

    private User getCurrentUser() {
        if (currentUser == null) {
            currentUser = facebook.getCurrentUser();
        }
        return currentUser;
    }

    ID getIdentifier() {
        User user = getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user not set");
        }
        return user.identifier;
    }

    String getNumberString() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        return facebook.getNumberString(identifier);
    }

    Profile getProfile() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        return facebook.getProfile(identifier);
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

    Uri getAvatarUrl() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        String avatar = facebook.getAvatar(identifier);
        if (avatar == null) {
            return SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher);
        }
        return Uri.parse(avatar);
    }

    String getNickname() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        return facebook.getNickname(identifier);
    }

    String getAccountTitle() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        String nickname = facebook.getNickname(identifier);
        String number = facebook.getNumberString(identifier);
        return nickname + " (" + number + ")";
    }

    String getAccountDesc() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        return identifier.address.toString();
    }
}
