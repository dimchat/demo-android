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

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.common.BackgroundThread;
import chat.dim.database.Database;
import chat.dim.filesys.ExternalStorage;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.image.Images;

public class GroupViewModel extends EntityViewModel {

    private static Bitmap drawLogo(ID identifier) throws IOException {
        List<ID> members = facebook.getMembers(identifier);
        if (members == null || members.size() < 1) {
            return null;
        }
        ContentResolver contentResolver = SechatApp.getInstance().getContentResolver();
        Images.Size size = new Images.Size(64, 64);
        List<Bitmap> avatars = new ArrayList<>();
        Uri uri;
        Bitmap img;
        int count = 0;
        for (ID item : members) {
            uri = UserViewModel.getAvatarUri(item);
            if (uri == null) {
                continue;
            }
            img = Images.bitmapFormUri(contentResolver, uri, size);
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
    private static void refreshLogo(ID identifier, String path) {
        try {
            Bitmap bitmap = drawLogo(identifier);
            if (bitmap == null) {
                return;
            }
            byte[] png = Images.png(bitmap);
            if (ExternalStorage.saveData(png, path)) {
                logos.put(identifier, Uri.parse(path));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Uri getLogoUri(ID identifier) {
        Uri uri = logos.get(identifier);
        if (uri != null) {
            return uri;
        }
        String path = Database.getEntityFilePath(identifier, "logo.png");

        // refresh group logo in background
        BackgroundThread.wait(() -> refreshLogo(identifier, path));

        if (ExternalStorage.exists(path)) {
            uri = Uri.parse(path);
            logos.put(identifier, uri);
            return uri;
        }
        return SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher_foreground);
    }

    private static final Map<ID, Uri> logos = new HashMap<>();
}
