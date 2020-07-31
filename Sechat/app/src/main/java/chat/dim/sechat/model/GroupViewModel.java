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
import chat.dim.common.BackgroundThread;
import chat.dim.database.Database;
import chat.dim.extension.GroupManager;
import chat.dim.filesys.ExternalStorage;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.image.Images;

public class GroupViewModel extends EntityViewModel {

    public static Group getGroup(ID identifier) {
        return facebook.getGroup(identifier);
    }

    //
    //  Members
    //

    public static void checkMembers(ID group) {
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() < 1) {
            BackgroundThread.wait(() -> (new GroupManager(group)).query());
        }
    }

    public static boolean existsMember(ID member, ID group) {
        return facebook.existsMember(member, group);
    }

    public static boolean addMember(ID member, ID group) {
        return facebook.addMember(member, group);
    }

    //
    //  Logo
    //

    private static Bitmap drawLogo(ID identifier) {
        List<ID> members = facebook.getMembers(identifier);
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
    public static String refreshLogo(ID identifier) {
        String path = Database.getEntityFilePath(identifier, "logo.png");
        BackgroundThread.wait(() -> {
            try {
                Bitmap bitmap = drawLogo(identifier);
                if (bitmap == null) {
                    return;
                }
                byte[] png = Images.png(bitmap);
                if (ExternalStorage.saveData(png, path)) {
                    logos.put(identifier, path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return path;
    }

    public static Bitmap getLogo(ID identifier) {
        String path = logos.get(identifier);
        if (path == null) {
            // refresh group logo in background
            path = refreshLogo(identifier);
        }
        if (ExternalStorage.exists(path)) {
            logos.put(identifier, path);
            return BitmapFactory.decodeFile(path);
        }
        return SechatApp.getInstance().getIcon();
    }

    private static final Map<ID, String> logos = new HashMap<>();
}
