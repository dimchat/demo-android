/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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
package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.filesys.ExternalStorage;

public class GroupTable extends ExternalStorage {

    private Map<ID, List<ID>> membersMap = new HashMap<>();

    // "/sdcard/chat.dim.sechat/mkm/{address}/members.js"

    private static String getMembersFilePath(ID group) {
        String address = group.address.toString();
        return root + separator
                + "mkm" + separator
                + address.substring(0, 2) + separator
                + address + separator
                + "members.js";
    }

    @SuppressWarnings("unchecked")
    private List<ID> loadMembers(ID group) {
        String path = getMembersFilePath(group);
        List<String> array;
        try {
            array = (List<String>) loadJSON(path);
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
        if (array == null || array.size() == 0) {
            return null;
        }
        List<ID> memberList = new ArrayList<>();
        ID member;
        for (String item : array) {
            // FIXME: get ID by facebook
            member = ID.getInstance(item);
            assert member.isValid() : "member error: " + item;
            if (memberList.contains(member)) {
                continue;
            }
            memberList.add(member);
        }
        // TODO: ensure the founder is at the front
        return memberList;
    }

    private boolean saveMembers(ID group) {
        List<ID> memberList = membersMap.get(group);
        if (memberList == null || memberList.size() == 0) {
            throw new NullPointerException("group members cannot be empty: " + group);
        }
        String path = getMembersFilePath(group);
        try {
            return saveJSON(memberList, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public ID getFounder(ID group) {
        // TODO: get founder of group
        return null;
    }

    public ID getOwner(ID group) {
        // TODO: get owner of group
        return null;
    }

    public List<ID> getMembers(ID group) {
        List<ID> members = membersMap.get(group);
        if (members == null) {
            members = loadMembers(group);
            if (members == null) {
                // no need to load again
                members = new ArrayList<>();
            }
            membersMap.put(group, members);
        }
        return members;
    }

    public boolean addMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        if (members.contains(member)) {
            return false;
        }
        members.add(member);
        return saveMembers(group);
    }

    public boolean removeMember(ID member, ID group) {
        List<ID> members = getMembers(group);
        if (!members.contains(member)) {
            return false;
        }
        members.remove(member);
        return saveMembers(group);
    }

    public boolean saveMembers(List<ID> members, ID group) {
        assert members.size() > 0 : "group members list empty";
        membersMap.put(group, members);
        return saveMembers(group);
    }
}
