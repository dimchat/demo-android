package chat.dim.sechat.group;

import android.arch.lifecycle.ViewModel;
import android.graphics.Bitmap;

import chat.dim.Group;
import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sechat.model.UserViewModel;

public class ParticipantViewModel extends ViewModel {

    private Group group = null;

    void setGroup(ID identifier) {
        Facebook facebook = Facebook.getInstance();
        group = facebook.getGroup(identifier);
    }

    Bitmap getLogo() {
        return GroupViewModel.getLogo(group.identifier);
    }

    String getName() {
        return EntityViewModel.getName(group.identifier);
    }

    String getOwnerName() {
        ID owner = getOwner();
        if (owner == null) {
            return null;
        }
        return UserViewModel.getUserTitle(owner);
    }
    private ID getOwner() {
        return group.getOwner();
    }
}
