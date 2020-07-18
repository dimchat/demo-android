package chat.dim.sechat.chatbox;

import android.arch.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

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

    public List<ID> getParticipants(ID identifier) {
        List<ID> participants = new ArrayList<>();
        if (identifier.isUser()) {
            participants.add(identifier);
        } else if (identifier.isGroup()) {
            ID owner = facebook.getOwner(identifier);
            if (owner != null) {
                participants.add(owner);
            }
            List<ID> members = facebook.getMembers(identifier);
            if (members != null) {
                for (ID item : members) {
                    if (participants.contains(item)) {
                        continue;
                    }
                    participants.add(item);
                }
            }
        }
        return participants;
    }
}
