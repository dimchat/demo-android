package chat.dim.sechat.chatbox.manage;

import java.util.ArrayList;
import java.util.List;

import chat.dim.GroupManager;
import chat.dim.User;
import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.model.ConversationDatabase;
import chat.dim.protocol.ID;
import chat.dim.sechat.model.EntityViewModel;

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
            Facebook facebook = Messenger.getInstance().getFacebook();
            User user = facebook.getCurrentUser();
            return facebook.isOwner(user.identifier, identifier);
        }
        return false;
    }

    List<ID> getParticipants(int count) {
        participants.clear();

        ID identifier = getIdentifier();
        if (identifier.isUser()) {
            participants.add(identifier);
        } else if (identifier.isGroup()) {
            ID owner = getFacebook().getOwner(identifier);
            if (owner != null) {
                --count;
                participants.add(owner);
            }
            List<ID> members = getFacebook().getMembers(identifier);
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
        GroupManager gm = new GroupManager(group);
        return gm.quit();
    }
}
