package chat.dim.database;

import java.util.List;

import chat.dim.mkm.ID;

public class GroupTable extends ExternalStorage {

    public static ID getFounder(ID group) {
        // TODO: get founder of group
        return null;
    }

    public static ID getOwner(ID group) {
        // TODO: get owner of group
        return null;
    }

    public static List<ID> getMembers(ID group) {
        // TODO: get members of group
        return null;
    }

    public static boolean existsMember(ID member, ID group) {
        // TODO: check whether exists the member
        return false;
    }
}
