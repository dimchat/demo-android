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
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.protocol.NetworkType;

public class GroupTable implements chat.dim.database.GroupTable {

    private final EntityDatabase entityDatabase;

    private GroupTable() {
        super();
        entityDatabase = EntityDatabase.getInstance();
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
        SQLiteDatabase db = entityDatabase.getReadableDatabase();
        if (db == null) {
            return null;
        }
        ID founder = null;
        String[] columns = {"founder"};
        String[] selectionArgs = {group.toString()};
        try (Cursor cursor = db.query(EntityDatabase.T_GROUP, columns, "gid=?", selectionArgs, null, null, null)) {
            if (cursor.moveToNext()) {
                founder = EntityDatabase.getID(cursor.getString(0));
            }
        }
        // TODO: check founder within members
        return founder;
    }

    @Override
    public ID getOwner(ID group) {
        SQLiteDatabase db = entityDatabase.getReadableDatabase();
        if (db == null) {
            return null;
        }
        ID owner = null;
        String[] columns = {"owner", "founder"};
        String[] selectionArgs = {group.toString()};
        try (Cursor cursor = db.query(EntityDatabase.T_GROUP, columns, "gid=?", selectionArgs, null, null, null)) {
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
        SQLiteDatabase db = entityDatabase.getReadableDatabase();
        if (db == null) {
            return null;
        }
        String[] columns = {"member"};
        String[] selectionArgs = {group.toString()};
        try (Cursor cursor = db.query(EntityDatabase.T_MEMBERS, columns, "gid=?", selectionArgs, null, null, null)) {
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
        SQLiteDatabase db = entityDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }
        ContentValues values = new ContentValues();
        values.put("gid", group.toString());
        values.put("member", member.toString());
        return db.insert(EntityDatabase.T_MEMBERS, null, values) >= 0;
    }

    @Override
    public boolean removeMember(ID member, ID group) {
        SQLiteDatabase db = entityDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }
        String[] whereArgs = {group.toString(), member.toString()};
        return db.delete(EntityDatabase.T_MEMBERS, "gid=? AND member=?", whereArgs) >= 0;
    }

    @Override
    public boolean saveMembers(List<ID> members, ID group) {
        SQLiteDatabase db = entityDatabase.getWritableDatabase();
        if (db == null) {
            return false;
        }
        // remove all members
        String[] whereArgs = {group.toString()};
        db.delete(EntityDatabase.T_MEMBERS, "gid=?", whereArgs);
        // add members one by one
        ContentValues values = new ContentValues();
        values.put("gid", group.toString());
        for (ID member : members) {
            values.put("member", member.toString());
            db.insert(EntityDatabase.T_MEMBERS, null, values);
        }
        return true;
    }
}
