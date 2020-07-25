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

import android.net.Uri;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;

public class UserViewModel extends EntityViewModel {

    public static User getUser(Object identifier) {
        return facebook.getUser(facebook.getID(identifier));
    }

    public static Uri getAvatarUri(ID identifier) {
        if (identifier != null) {
            String avatar = facebook.getAvatar(identifier);
            if (avatar != null) {
                return Uri.parse(avatar);
            }
        }
        return SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher_round);
    }
    public Uri getAvatarUrl() {
        return getAvatarUri(getIdentifier());
    }

    public static String getNickname(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        return facebook.getNickname(identifier);
    }
    public String getNickname() {
        return getNickname(getIdentifier());
    }

    public static String getUserTitle(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("current user not set");
        }
        String name = EntityViewModel.getName(identifier);
        String number = facebook.getNumberString(identifier);
        return name + " (" + number + ")";
    }
    public String getUserTitle() {
        return getUserTitle(getIdentifier());
    }
}