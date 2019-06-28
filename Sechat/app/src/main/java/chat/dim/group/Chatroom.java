package chat.dim.group;

import java.util.List;

import chat.dim.mkm.Group;
import chat.dim.mkm.entity.ID;

public class Chatroom extends Group {

    public Chatroom(ID identifier) {
        super(identifier);
    }

    public List<ID> getAdmins() {
        ChatroomDataSource dataSource = (ChatroomDataSource) this.dataSource;
        return dataSource.getAdmins(identifier);
    }
}
