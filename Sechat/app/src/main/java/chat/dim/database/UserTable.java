package chat.dim.database;

import java.util.ArrayList;
import java.util.List;

import chat.dim.mkm.entity.ID;

public class UserTable extends Resource {

    private static List<ID> contactList = new ArrayList<>();

    public static void reloadData(ID user) {
        // TODO: reload contacts for current user
        contactList.add(ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj"));
        contactList.add(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));
    }

    public static List<ID> getContacts(ID user) {
        return contactList;
    }
}
