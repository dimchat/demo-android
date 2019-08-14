package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import chat.dim.client.Facebook;
import chat.dim.mkm.ID;

public class ContactTable extends ExternalStorage {

    private static List<ID> contactList = new ArrayList<>();

    // "/sdcard/chat.dim.sechat/mkm/{address}/contacts.js"

    private static String getContactsFilePath(ID user) {
        return root + "/mkm/" + user.address + "/contacts.js";
    }

    @SuppressWarnings("unchecked")
    static void reloadContacts(ID user) {
        // 1. clear contacts
        // FIXME: don't clear contact list when testing
        //contactList.clear();

        // 2. reading contacts file in the user's directory
        String path = getContactsFilePath(user);
        List<String> contacts = null;
        try {
            contacts = (List<String>) readJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (contacts == null) {
            return;
        }

        // add contacts
        Facebook facebook = Facebook.getInstance();
        ID contact;
        for (String item : contacts) {
            contact = facebook.getID(item);
            if (contactList.contains(contact)) {
                continue;
            }
            contactList.add(contact);
        }

        sortContacts();
    }

    private static boolean saveContacts(ID user) {
        String path = getContactsFilePath(user);
        try {
            return writeJSON(contactList, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void sortContacts() {
        // TODO: sort contact list
    }

    public static boolean addContact(ID contact, ID user) {
        if (contactList.contains(contact)) {
            return false;
        }
        boolean added = contactList.add(contact);
        sortContacts();
        return added && saveContacts(user);
    }

    public static boolean removeContact(ID contact, ID user) {
        if (!contactList.contains(contact)) {
            return false;
        }
        boolean removed = contactList.remove(contact);
        return removed && saveContacts(user);
    }

    public static List<ID> getContacts(ID user) {
        return contactList;
    }

    static {
        // FIXME: test data
        contactList.add(ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj"));
        contactList.add(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));
    }
}
