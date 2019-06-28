package chat.dim.group;

import java.util.List;

import chat.dim.mkm.GroupDataSource;
import chat.dim.mkm.entity.ID;

public interface ChatroomDataSource extends GroupDataSource {

    /**
     *  Get chatroom admins list
     *
     * @param chatroom - chatroom ID
     * @return admins list (ID)
     */
    List<ID> getAdmins(ID chatroom);
}
