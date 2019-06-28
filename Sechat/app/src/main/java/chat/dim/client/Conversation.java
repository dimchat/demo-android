package chat.dim.client;

import java.util.List;

import chat.dim.core.Barrack;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.Account;
import chat.dim.mkm.Group;
import chat.dim.mkm.entity.Entity;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.NetworkType;

public class Conversation {
    public static int PersonalChat = NetworkType.Main.value;
    public static int GroupChat = NetworkType.Group.value;

    private final Entity entity;

    public final int type;
    public final ID identifier;
    public final String name;
    public final String title;

    public ConversationDataSource dataSource;

    public Conversation(Entity entity) {
        super();
        this.entity = entity;
        this.type = entity.getType().isCommunicator() ? PersonalChat : GroupChat;
        this.identifier = entity.identifier;
        this.name = entity.getName();
        this.title = (type == GroupChat) ? getGroupTitle((Group) entity) : getPersonTitle((Account) entity);
    }

    // Person: "xxx"
    private String getPersonTitle(Account person) {
        return person.getName();
    }
    // Group: "yyy (123)"
    private String getGroupTitle(Group group) {
        List<ID> members = group.getMembers();
        int count = members == null ? 0 : members.size();
        return group.getName() + " (" + count + ")";
    }

    public int numberOfMessage() {
        return dataSource.numberOfMessages(this);
    }

    public InstantMessage messageAtIndex(int index) {
        return dataSource.messageAtIndex(index, this);
    }

    public boolean insertMessage(InstantMessage iMsg) {
        return dataSource.insertMessage(iMsg, this);
    }

    public boolean removeMessage(InstantMessage iMsg) {
        return dataSource.removeMessage(iMsg, this);
    }

    public boolean withdrawMessage(InstantMessage iMsg) {
        return dataSource.withdrawMessage(iMsg, this);
    }
}
