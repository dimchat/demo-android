package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModel;
import android.net.Uri;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
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
            return null;
        }
        return user.identifier;
    }

    String getNumberString() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return facebook.getNumberString(user.identifier);
    }

    Uri getAvatarUrl() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        String avatar = facebook.getAvatar(user.identifier);
        if (avatar == null) {
            return SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher);
        }
        return Uri.parse(avatar);
    }

    String getNickname() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return facebook.getNickname(user.identifier);
    }

    String getAccountTitle() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        String nickname = facebook.getNickname(user.identifier);
        String number = facebook.getNumberString(user.identifier);
        return nickname + " (" + number + ")";
    }

    String getAccountDesc() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.identifier.address.toString();
    }
}
