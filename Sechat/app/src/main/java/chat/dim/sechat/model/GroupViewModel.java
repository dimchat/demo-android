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
import android.graphics.BitmapFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Group;
import chat.dim.ID;
import chat.dim.User;
import chat.dim.extension.GroupManager;
import chat.dim.filesys.ExternalStorage;
import chat.dim.sechat.SechatApp;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.image.Images;

public class GroupViewModel extends EntityViewModel {

    public static Group getGroup(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        return facebook.getGroup(group);
    }
    public Group getGroup() {
        return getGroup(getIdentifier());
    }

    //
    //  Administrators
    //

    public static ID getOwner(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        return facebook.getOwner(group);
    }
    public ID getOwner() {
        return getOwner(getIdentifier());
    }

    public static String getOwnerName(ID group) {
        ID owner = getOwner(group);
        if (owner == null) {
            return null;
        }
        return UserViewModel.getUserTitle(owner);
    }
    public String getOwnerName() {
        return getOwnerName(getIdentifier());
    }

    public static boolean isAdmin(ID user, ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        return facebook.isOwner(user, group);
    }
    public boolean isAdmin(ID user) {
        return isAdmin(user, getIdentifier());
    }

    public static boolean isAdmin(User user, ID group) {
        if (user == null) {
            return false;
        }
        return isAdmin(user.identifier, group);
    }
    public boolean isAdmin(User user) {
        return isAdmin(user, getIdentifier());
    }

    //
    //  Members
    //

    public static void checkMembers(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() < 1) {
            BackgroundThreads.wait(() -> (new GroupManager(group)).query());
        }
    }
    public void checkMembers() {
        checkMembers(getIdentifier());
    }

    public static boolean existsMember(ID member, ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        return facebook.existsMember(member, group);
    }
    public boolean existsMember(ID member) {
        return existsMember(member, getIdentifier());
    }

    public static boolean addMember(ID member, ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        return facebook.addMember(member, group);
    }
    public boolean addMember(ID member) {
        return addMember(member, getIdentifier());
    }

    public static List<ID> getMembers(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        return facebook.getMembers(group);
    }
    public List<ID> getMembers() {
        return getMembers(getIdentifier());
    }

    //
    //  Logo
    //

    private static Bitmap drawLogo(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() < 1) {
            return null;
        }
        List<Bitmap> avatars = new ArrayList<>();
        Bitmap img;
        int count = 0;
        for (ID item : members) {
            img = UserViewModel.getAvatar(item);
            if (img == null) {
                continue;
            }
            if (++count > 9) {
                break;
            }
            avatars.add(img);
        }
        return Images.tiles(avatars, new Images.Size(128, 128));
    }
    public static String refreshLogo(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        String path;
        try {
            path = ExternalStorage.getEntityFilePath(group, "logo.png");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        BackgroundThreads.wait(() -> {
            try {
                Bitmap bitmap = drawLogo(group);
                if (bitmap == null) {
                    return;
                }
                byte[] png = Images.png(bitmap);
                if (ExternalStorage.saveData(png, path)) {
                    logos.put(group, path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return path;
    }
    public String refreshLogo() {
        return refreshLogo(getIdentifier());
    }

    public static Bitmap getLogo(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        String path = logos.get(group);
        if (path == null) {
            // refresh group logo in background
            path = refreshLogo(group);
        }
        if (ExternalStorage.exists(path)) {
            logos.put(group, path);
            return BitmapFactory.decodeFile(path);
        }
        return SechatApp.getInstance().getIcon();
    }
    public Bitmap getLogo() {
        return getLogo(getIdentifier());
    }

    private static final Map<ID, String> logos = new HashMap<>();
}
