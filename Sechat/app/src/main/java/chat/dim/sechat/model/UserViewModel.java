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
package chat.dim.sechat.model;

import android.graphics.Bitmap;

import java.io.IOException;
import java.util.List;

import chat.dim.Entity;
import chat.dim.User;
import chat.dim.protocol.ID;
import chat.dim.protocol.LoginCommand;
import chat.dim.sechat.SechatApp;
import chat.dim.sqlite.dim.LoginTable;
import chat.dim.ui.image.Images;

public class UserViewModel extends EntityViewModel {

    public static User getCurrentUser() {
        return facebook.getCurrentUser();
    }

    public static User getUser(Object identifier) {
        if (identifier == null) {
            throw new NullPointerException("user ID empty");
        }
        return facebook.getUser(Entity.parseID(identifier));
    }
    public User getUser() {
        return getUser(getIdentifier());
    }

    public static Bitmap getAvatar(ID identifier) {
        if (identifier != null) {
            String avatar = facebook.getAvatar(identifier);
            if (avatar != null) {
                try {
                    return Images.bitmapFromPath(avatar, new Images.Size(128, 128));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return SechatApp.getInstance().getIcon();
    }
    public Bitmap getAvatar() {
        return getAvatar(getIdentifier());
    }

    public static String getNickname(Object identifier) {
        if (identifier == null) {
            throw new NullPointerException("user ID empty");
        }
        return facebook.getNickname(identifier);
    }
    public String getNickname() {
        return getNickname(getIdentifier());
    }

    public static String getUsername(Object identifier) {
        if (identifier == null) {
            throw new NullPointerException("user ID empty");
        }
        return facebook.getUsername(identifier);
    }
    public String getUsername() {
        return getUsername(getIdentifier());
    }

    public static String getUserTitle(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("user ID empty");
        }
        return EntityViewModel.getName(identifier);
    }
    public String getUserTitle() {
        return getUserTitle(getIdentifier());
    }

    //
    //  Login
    //
    public static LoginCommand getLoginCommand(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("user ID empty");
        }
        LoginTable db = LoginTable.getInstance();
        return db.getLoginCommand(identifier);
    }

    //
    //  Contacts
    //

    public static List<ID> getContacts(ID user) {
        if (user == null) {
            throw new NullPointerException("user ID empty");
        }
        return facebook.getContacts(user);
    }
    public List<ID> getContacts() {
        User user = getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user not set");
        }
        return getContacts(user.identifier);
    }

    public boolean addContact(ID contact) {
        User user = getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user not set");
        }
        return facebook.addContact(contact, user.identifier);
    }

    public boolean removeContact(ID contact) {
        User user = getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user not set");
        }
        return facebook.removeContact(contact, user.identifier);
    }
}
