package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModel;

import chat.dim.common.Facebook;
import chat.dim.mkm.LocalUser;

class AccountViewModel extends ViewModel {

    private Facebook facebook = Facebook.getInstance();

    private LocalUser currentUser = null;

    private LocalUser getCurrentUser() {
        if (currentUser == null) {
            currentUser = facebook.database.getCurrentUser();
        }
        return currentUser;
    }

    String getAccountTitle() {
        LocalUser user = getCurrentUser();
        if (user == null) {
            return null;
        }
        String nickname = facebook.getNickname(user.identifier);
        String number = facebook.getNumberString(user.identifier);
        return nickname + " " + number;
    }

    String getAccountDesc() {
        LocalUser user = getCurrentUser();
        if (user == null) {
            return null;
        }
        return user.identifier.toString();
    }
}
