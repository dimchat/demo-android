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

import chat.dim.GlobalVariable;
import chat.dim.GroupManager;
import chat.dim.SharedFacebook;
import chat.dim.filesys.EntityStorage;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;
import chat.dim.mkm.Entity;
import chat.dim.mkm.Group;
import chat.dim.protocol.ID;
import chat.dim.sechat.SechatApp;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.image.Images;

public class GroupViewModel extends EntityViewModel {

    @Override
    protected Entity getEntity() {
        return getGroup();
    }
    public Group getGroup() {
        Entity entity = super.getEntity();
        if (entity instanceof Group) {
            return (Group) entity;
        }
        return null;
    }

    public String getOwnerName() {
        Group group = getGroup();
        if (group == null) {
            return null;
        }
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        ID owner = group.getOwner();
        return facebook.getName(owner);
    }

    public List<ID> getMembers() {
        Group group = getGroup();
        if (group == null) {
            return null;
        }
        return group.getMembers();
    }

    public void checkMembers() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return;
        }
        List<ID> members = getMembers();
        if (members == null || members.size() < 1) {
            BackgroundThreads.wait(() -> {
                GroupManager manager = GroupManager.getInstance();
                boolean ok = manager.query(identifier);
                assert ok : "failed to query group info: " + identifier;
            });
        }
    }

    //
    //  Logo
    //

    private static Bitmap drawLogo(ID group) {
        if (group == null) {
            throw new NullPointerException("group ID empty");
        }
        GroupManager manager = GroupManager.getInstance();
        List<ID> members = manager.getMembers(group);
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
        String path = EntityStorage.getEntityFilePath(group, "logo.png");
        BackgroundThreads.wait(() -> {
            try {
                Bitmap bitmap = drawLogo(group);
                if (bitmap == null) {
                    return;
                }
                byte[] png = Images.png(bitmap);
                if (ExternalStorage.saveBinary(png, path) == png.length) {
                    logos.put(group, path);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return path;
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
        if (Paths.exists(path)) {
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
