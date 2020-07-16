package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModel;
import android.net.Uri;

import chat.dim.User;
import chat.dim.model.Facebook;

public class AccountViewModel extends ViewModel {

    private Facebook facebook = Facebook.getInstance();

    private User currentUser = null;

    private User getCurrentUser() {
        if (currentUser == null) {
            currentUser = facebook.getCurrentUser();
        }
        return currentUser;
    }

    Uri getAvatarUrl() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        String avatar = facebook.getAvatar(user.identifier);
        if (avatar == null) {
            return null;
        }
        return Uri.parse(avatar);
    }

    String getAccountTitle() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        String nickname = facebook.getNickname(user.identifier);
        String number = facebook.getNumberString(user.identifier);
        return nickname + " " + number;
    }

    String getAccountDesc() {
        User user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.identifier.toString();
    }
}
