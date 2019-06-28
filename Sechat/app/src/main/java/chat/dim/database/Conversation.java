package chat.dim.database;

import java.util.List;

import chat.dim.client.Facebook;
import chat.dim.mkm.Group;
import chat.dim.mkm.entity.Entity;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.NetworkType;

public class Conversation {
    public static int PersonalChat = NetworkType.Main.value;
    public static int GroupChat = NetworkType.Group.value;

    private final Entity entity;
    public final ID identifier;
    public final int type;

    public Conversation(Entity entity) {
        super();
        this.entity = entity;
        this.identifier = entity.identifier;
        this.type = getType(entity);
    }

//    public Conversation(ID identifier) {
//        this(identifier.getType().isGroup() ?
//                Facebook.getInstance().getGroup(identifier) :
//                Facebook.getInstance().getAccount(identifier));
//    }

    private int getType(Entity entity) {
        NetworkType type = entity.getType();
        if (type.isGroup()) {
            return GroupChat;
        }
        return PersonalChat;
    }

    public String getName() {
        return entity.getName();
    }

    public String getTitle() {
        String name = entity.getName();
        NetworkType type = entity.getType();
        if (type.isGroup()) {
            Group group = (Group) entity;
            List<ID> members = group.getMembers();
            int count = (members == null) ? 0 : members.size();
            // Group: "yyy (123)"
            return name + " (" + count + ")";
        }
        // Person: "xxx"
        return name;
    }
}
