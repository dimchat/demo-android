package chat.dim.sechat.chatbox;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.extension.GroupManager;
import chat.dim.model.ConversationDatabase;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sechat.model.UserViewModel;

public class ChatManageViewModel extends EntityViewModel {

    public static int MAX_ITEM_COUNT = 2 * 3 * 5;

    public static int UNLIMITED = -1;

    private List<ID> participants = new ArrayList<>();

    int getMaxItemCount() {
        if (isGroupAdmin()) {
            return MAX_ITEM_COUNT - 2;
        } else {
            return MAX_ITEM_COUNT - 1;
        }
    }

    boolean isGroupAdmin() {
        ID identifier = getIdentifier();
        if (identifier != null && identifier.isGroup()) {
            User user = UserViewModel.getCurrentUser();
            return GroupViewModel.isAdmin(user, identifier);
        }
        return false;
    }

    public List<ID> getParticipants(int count) {
        participants.clear();

        if (identifier.isUser()) {
            participants.add(identifier);
        } else if (identifier.isGroup()) {
            ID owner = facebook.getOwner(identifier);
            if (owner != null) {
                --count;
                participants.add(owner);
            }
            List<ID> members = facebook.getMembers(identifier);
            if (members != null) {
                for (ID item : members) {
                    if (participants.contains(item)) {
                        continue;
                    }
                    if (--count == UNLIMITED) {
                        break;
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

    boolean quitGroup(ID group) {
        clearHistory(group);

        User user = facebook.getCurrentUser();
        assert user != null : "failed to get current user";
        GroupManager gm = new GroupManager(group);
        return gm.quit(user.identifier);
    }
}
