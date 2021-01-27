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
import android.net.Uri;

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

    @Override
    protected Entity getEntity() {
        return getUser();
    }
    public User getUser() {
        Entity entity = super.getEntity();
        if (entity instanceof User) {
            return (User) entity;
        }
        return null;
    }

    public static Bitmap getAvatar(ID identifier) {
        String avatar = getFacebook().getAvatar(identifier);
        if (avatar != null) {
            try {
                return Images.bitmapFromPath(avatar, new Images.Size(128, 128));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return SechatApp.getInstance().getIcon();
    }
    public Bitmap getAvatar() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        return getAvatar(identifier);
    }

    public Uri getAvatarUri() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        String avatar = getFacebook().getAvatar(identifier);
        if (avatar == null) {
            return null;
        }
        return Uri.parse(avatar);
    }

    public boolean containsContact(ID contact) {
        User user = getFacebook().getCurrentUser();
        if (user == null) {
            return false;
        }
        List<ID> contacts = getFacebook().getContacts(user.identifier);
        if (contacts == null) {
            return false;
        }
        return contacts.contains(contact);
    }

    //
    //  Login
    //
    public static LoginCommand getLoginCommand(ID identifier) {
        LoginTable db = LoginTable.getInstance();
        return db.getLoginCommand(identifier);
    }
}
