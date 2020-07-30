package chat.dim.sechat.chatbox;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.sechat.model.EntityViewModel;

public class ChatManageViewModel extends EntityViewModel {

    private static Facebook facebook = Facebook.getInstance();

    static final ID INVITE_BTN_ID = ID.getInstance("invite@anywhere");
    static final ID EXPEL_BTN_ID = ID.getInstance("expel@anywhere");

    static boolean isAdmin(User user, ID group) {
        if (user == null) {
            return false;
        }
        return isAdmin(user.identifier, group);
    }
    private static boolean isAdmin(ID user, ID group) {
        return facebook.isOwner(user, group);
    }

    List<ID> getParticipants(ID identifier) {
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

    boolean clearHistory(ID identifier) {
        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        return msgDB.clearConversation(identifier);
    }
}
