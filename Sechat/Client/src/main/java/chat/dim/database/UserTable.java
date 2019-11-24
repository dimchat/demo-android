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
import java.util.List;

import chat.dim.ID;
import chat.dim.model.Facebook;

public class UserTable extends ExternalStorage {

    private List<ID> userList = null;

    // "/sdcard/chat.dim.sechat/dim/users.js"

    private static String getUsersFilePath() {
        return root + "/dim/users.js";
    }

    private boolean saveUsers() {
        assert userList != null;
        // save into storage
        String path = getUsersFilePath();
        try {
            return writeJSON(userList, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean loadUsers() {
        assert userList == null;
        userList = new ArrayList<>();
        // loading from storage
        String path = getUsersFilePath();
        List list;
        try {
            list = (List) readJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        if (list == null || list.size() == 0) {
            return false;
        }
        Facebook facebook = Facebook.getInstance();
        ID user;
        for (Object item : list) {
            user = facebook.getID(item);
            if (userList.contains(user)) {
                continue;
            }
            userList.add(user);
        }
        return true;
    }

    public List<ID> allUsers() {
        if (userList == null && !loadUsers()) {
            return null;
        }
        return userList;
    }

    public boolean addUser(ID user) {
        if (userList == null) {
            loadUsers();
        }
        if (userList.contains(user)) {
            return false;
        }
        userList.add(user);
        return saveUsers();
    }

    public boolean removeUser(ID user) {
        if (userList == null) {
            loadUsers();
        }
        if (!userList.contains(user)) {
            return false;
        }
        boolean removed = userList.remove(user);
        return removed && saveUsers();
    }

    public void setCurrentUser(ID user) {
        if (userList == null) {
            loadUsers();
        }
        int index = userList.indexOf(user);
        if (index == 0) {
            // already the first user
            return;
        }
        if (index > 0) {
            // already exists, but not the first user
            userList.remove(user);
        }
        userList.add(0, user);
        saveUsers();
    }

    public ID getCurrentUser() {
        if (userList == null) {
            loadUsers();
        }
        if (userList.size() > 0) {
            return userList.get(0);
        }
        return null;
    }
}
