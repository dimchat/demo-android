package chat.dim.database;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import chat.dim.client.Facebook;
import chat.dim.mkm.ID;

public class AddressNameTable extends ExternalStorage {

    private static Map<String, ID> ansTable = loadRecords();

    // "/sdcard/chat.dim.sechat/dim/ans.txt"

    private static String getAnsFilePath() {
        return root + "/dim/ans.txt";
    }

    private static boolean cacheRecord(String name, ID identifier) {
        if (name.length() == 0) {
            return false;
        }
        assert identifier.isValid();
        ansTable.put(name, identifier);
        return true;
    }

    private static Map<String, ID> loadRecords() {
        Map<String, ID> dictionary = new HashMap<>();
        String path = getAnsFilePath();
        // loading ANS records
        String text;
        try {
            text = readText(path);
        } catch (IOException e) {
            e.printStackTrace();
            return dictionary;
        }
        if (text == null || text.length() == 0) {
            return dictionary;
        }
        Facebook facebook = Facebook.getInstance();
        ID moky = facebook.getID("moky@4DnqXWdTV8wuZgfqSCX9GjE2kNq7HJrUgQ");
        String[] lines = text.split("[\\r\\n]+");
        String[] pair;
        for (String record : lines) {
            pair = record.split("\\t");
            if (pair.length != 2) {
                // invalid record
                continue;
            }
            dictionary.put(pair[0], facebook.getID(pair[1]));
        }
        // Reserved names
        dictionary.put("all", ID.EVERYONE);
        dictionary.put(ID.EVERYONE.toString(), ID.EVERYONE);
        dictionary.put(ID.ANYONE.toString(), ID.ANYONE);
        dictionary.put("owner", ID.ANYONE);
        dictionary.put("founder", moky);
        return dictionary;
    }

    private static boolean saveRecords(Map<String, ID> caches) {
        StringBuilder text = new StringBuilder();
        Set<String> allKeys = caches.keySet();
        ID identifier;
        for (String name : allKeys) {
            identifier = caches.get(name);
            assert identifier != null;
            text.append(name);
            text.append("\t");
            text.append(identifier.toString());
            text.append("\n");
        }
        // saving ANS records
        String path = getAnsFilePath();
        try {
            return writeText(text.toString(), path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     *  Save ANS record
     *
     * @param name - short name
     * @param identifier - user ID
     * @return true on success
     */
    public static boolean saveRecord(String name, ID identifier) {
        if (!cacheRecord(name, identifier)) {
            return false;
        }
        // save to local storage
        return saveRecords(ansTable);
    }

    /**
     *  Get ID by short name
     *
     * @param name - short name
     * @return user ID
     */
    public static ID record(String name) {
        return ansTable.get(name.toLowerCase());
    }

    /**
     *  Get all short names with this ID
     *
     * @param identifier - user ID
     * @return all short names pointing to this same ID
     */
    public static Set<String> names(String identifier) {
        Set<String> allKeys = ansTable.keySet();
        // all names
        if (identifier.equals("*")) {
            return allKeys;
        }
        Facebook facebook = Facebook.getInstance();
        ID target = facebook.getID(identifier);
        // get keys with the same value
        Set<String> keys = new HashSet<>();
        ID value;
        for (String key : allKeys) {
            value = ansTable.get(key);
            if (value != null && value.equals(target)) {
                keys.add(key);
            }
        }
        return keys;
    }
}
