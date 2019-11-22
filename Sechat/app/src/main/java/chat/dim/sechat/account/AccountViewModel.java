package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModel;

import chat.dim.common.Facebook;
import chat.dim.mkm.User;

class AccountViewModel extends ViewModel {

    private Facebook facebook = Facebook.getInstance();

    private User currentUser = null;

    private User getCurrentUser() {
        if (currentUser == null) {
            currentUser = facebook.getCurrentUser();
        }
        return currentUser;
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
