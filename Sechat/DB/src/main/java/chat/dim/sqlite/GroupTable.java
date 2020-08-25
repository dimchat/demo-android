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
import chat.dim.protocol.NetworkType;

public class GroupTable extends DataTable implements chat.dim.database.GroupTable {

    private GroupTable() {
        super(EntityDatabase.getInstance());
    }

    private static GroupTable ourInstance;
    public static GroupTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new GroupTable();
        }
        return ourInstance;
    }

    //
    //  chat.dim.database.GroupTable
    //

    @Override
    public ID getFounder(ID group) {
        ID founder = null;
        String[] columns = {"founder"};
        String[] selectionArgs = {group.toString()};
        try (Cursor cursor = query(EntityDatabase.T_GROUP, columns, "gid=?", selectionArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                founder = EntityDatabase.getID(cursor.getString(0));
            }
        }
        // TODO: check founder within members
        return founder;
    }

    @Override
    public ID getOwner(ID group) {
        ID owner = null;
        String[] columns = {"owner", "founder"};
        String[] selectionArgs = {group.toString()};
        try (Cursor cursor = query(EntityDatabase.T_GROUP, columns, "gid=?", selectionArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                owner = EntityDatabase.getID(cursor.getString(0));
                if (owner == null && group.getType() == NetworkType.Polylogue.value) {
                    // Polylogue's owner is its founder
                    owner = EntityDatabase.getID(cursor.getString(1));
                }
            }
        }
        return owner;
    }

    @Override
    public List<ID> getMembers(ID group) {
        String[] columns = {"member"};
        String[] selectionArgs = {group.toString()};
        try (Cursor cursor = query(EntityDatabase.T_MEMBER, columns, "gid=?", selectionArgs, null, null, null)) {
            List<ID> members = new ArrayList<>();
            ID identifier;
            while (cursor.moveToNext()) {
                identifier = EntityDatabase.getID(cursor.getString(0));
                if (identifier != null) {
                    members.add(identifier);
                }
            }
            return members;
        }
    }

    @Override
    public boolean addMember(ID member, ID group) {
        ContentValues values = new ContentValues();
        values.put("gid", group.toString());
        values.put("member", member.toString());
        return insert(EntityDatabase.T_MEMBER, null, values) >= 0;
    }

    @Override
    public boolean removeMember(ID member, ID group) {
        String[] whereArgs = {group.toString(), member.toString()};
        return delete(EntityDatabase.T_MEMBER, "gid=? AND member=?", whereArgs) > 0;
    }

    @Override
    public boolean saveMembers(List<ID> newMembers, ID group) {
        int count = 0;
        // remove expelled member(s)
        List<ID> oldMembers = getMembers(group);
        for (ID item : oldMembers) {
            if (newMembers.contains(item)) {
                continue;
            }
            if (removeMember(item, group)) {
                ++count;
            }
        }
        // insert new member(s)
        for (ID item : newMembers) {
            if (oldMembers.contains(item)) {
                continue;
            }
            if (addMember(item, group)) {
                ++count;
            }
        }
        return count > 0;
    }

    @Override
    public boolean removeGroup(ID group) {
        String[] whereArgs = {group.toString()};
        boolean ok1 = delete(EntityDatabase.T_MEMBER, "gid=?", whereArgs) > 0;
        boolean ok2 = delete(EntityDatabase.T_GROUP, "gid=?", whereArgs) > 0;
        return ok1 || ok2;
    }
}
