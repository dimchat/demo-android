/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.sqlite;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;

public class ContactTable extends DataTable implements chat.dim.database.ContactTable {

    private ContactTable() {
        super(EntityDatabase.getInstance());
    }

    private static ContactTable ourInstance;
    public static ContactTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new ContactTable();
        }
        return ourInstance;
    }

    //
    //  chat.dim.database.ContactTable
    //

    @Override
    public List<ID> getContacts(ID user) {
        String[] columns = {"contact"};
        String[] selectionArgs = {user.toString()};
        try (Cursor cursor = query(EntityDatabase.T_CONTACT, columns, "uid=?", selectionArgs, null, null, null)) {
            List<ID> contacts = new ArrayList<>();
            ID identifier;
            while (cursor.moveToNext()) {
                identifier = EntityDatabase.getID(cursor.getString(0));
                if (identifier != null) {
                    contacts.add(identifier);
                }
            }
            return contacts;
        }
    }

    @Override
    public boolean addContact(ID contact, ID user) {
        ContentValues values = new ContentValues();
        values.put("uid", user.toString());
        values.put("contact", contact.toString());
        return insert(EntityDatabase.T_CONTACT, null, values) >= 0;
    }

    @Override
    public boolean removeContact(ID contact, ID user) {
        String[] whereArgs = {user.toString(), contact.toString()};
        return delete(EntityDatabase.T_CONTACT, "uid=? AND contact=?", whereArgs) > 0;
    }

    @Override
    public boolean saveContacts(List<ID> newContacts, ID user) {
        int count = 0;
        // remove expelled contact(s)
        List<ID> oldContacts = getContacts(user);
        for (ID item : oldContacts) {
            if (newContacts.contains(item)) {
                continue;
            }
            if (removeContact(item, user)) {
                ++count;
            }
        }
        // insert new contact(s)
        for (ID item : newContacts) {
            if (oldContacts.contains(item)) {
                continue;
            }
            if (addContact(item, user)) {
                ++count;
            }
        }
        return count > 0;
    }
}