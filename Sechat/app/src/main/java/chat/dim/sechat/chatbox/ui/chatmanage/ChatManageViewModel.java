package chat.dim.sechat.chatbox.ui.chatmanage;

import android.arch.lifecycle.ViewModel;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.model.Facebook;

public class ChatManageViewModel extends ViewModel {

    private static Facebook facebook = Facebook.getInstance();

    public String getName(ID identifier) {
        String name;
        Profile profile = facebook.getProfile(identifier);
        if (profile != null) {
            name = profile.getName();
            if (name != null) {
                return name;
            }
        }
        name = identifier.name;
        if (name != null) {
            return name;
        }
        return identifier.toString();
    }

    public String getNumberString(ID identifier) {
        return facebook.getNumberString(identifier);
    }
}
