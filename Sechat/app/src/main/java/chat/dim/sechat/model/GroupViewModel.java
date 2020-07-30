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

    private static Bitmap drawLogo(ID identifier) {
        List<ID> members = facebook.getCacheMembers(identifier);
        if (members != null && members.size() > 0) {
            List<String> avatars = new ArrayList<>();
            String url;
            for (ID item : members) {
                url = facebook.getAvatar(item);
                if (url != null && url.length() > 0) {
                    avatars.add(url);
                }
            }
            return Images.tiles(avatars);
        }
        return null;
    }
    private static void refreshLogo(ID identifier, String path) {
        Bitmap bitmap = drawLogo(identifier);
        if (bitmap != null) {
            byte[] png = Images.png(bitmap);
            try {
                ExternalStorage.saveData(png, path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Uri getLogoUri(ID identifier) {
        Uri uri = logos.get(identifier);
        if (uri != null) {
            return uri;
        }
        String path = Database.getEntityFilePath(identifier, "avatar.png");

        // refresh group logo in background
        BackgroundThread.run(() -> refreshLogo(identifier, path));

        if (ExternalStorage.exists(path)) {
            uri = Uri.parse(path);
            logos.put(identifier, uri);
            return uri;
        }
        return SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher_foreground);
    }

    private static final Map<ID, Uri> logos = new HashMap<>();
}
