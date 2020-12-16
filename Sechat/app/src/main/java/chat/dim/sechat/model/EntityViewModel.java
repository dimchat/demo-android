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

import androidx.lifecycle.ViewModel;

import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.threading.BackgroundThreads;

public class EntityViewModel extends ViewModel {

    protected static Facebook facebook = Facebook.getInstance();

    protected ID identifier = null;

    //
    //  ID
    //
    public ID getIdentifier() {
        return identifier;
    }
    public void setIdentifier(ID identifier) {
        this.identifier = identifier;
    }

    //
    //  Address string
    //
    public static String getAddressString(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("entity ID empty");
        }
        return identifier.getAddress().toString();
    }
    public String getAddressString() {
        return getAddressString(getIdentifier());
    }

    //
    //  Name string
    //
    private static String getName(ID identifier, Document profile) {
        String name = profile.getName();
        if (name != null) {
            return name;
        }
        name = identifier.getName();
        if (name != null) {
            return name;
        }
        return identifier.toString();
    }


    public static String getName(ID identifier) {
        return getName(identifier, getDocument(identifier, "*"));
    }
    public String getName() {
        return getName(identifier, getDocument("*"));
    }

    //
    //  Entity Document
    //
    public static Document getDocument(ID identifier, String type) {
        if (identifier == null) {
            throw new NullPointerException("entity ID empty");
        }
        return facebook.getDocument(identifier, type);
    }
    public Document getDocument(String type) {
        return getDocument(getIdentifier(), type);
    }

    public void refreshProfile() {
        BackgroundThreads.wait(() -> {
            Document profile = getDocument("*");
            if (facebook.isEmpty(profile) || facebook.isExpired(profile)) {
                Messenger messenger = Messenger.getInstance();
                messenger.queryProfile(identifier);
            }
        });
    }
}
