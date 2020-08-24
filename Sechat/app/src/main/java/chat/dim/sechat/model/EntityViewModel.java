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

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.model.Facebook;

public class EntityViewModel extends ViewModel {

    protected static Facebook facebook = Facebook.getInstance();

    protected ID identifier = null;

    //
    //  ID
    //
    public static ID getID(Object identifier) {
        return facebook.getID(identifier);
    }
    public ID getIdentifier() {
        return identifier;
    }
    public void setIdentifier(ID identifier) {
        this.identifier = identifier;
    }

    //
    //  Number string
    //
    public static String getNumberString(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("entity ID empty");
        }
        return facebook.getNumberString(identifier);
    }
    public String getNumberString() {
        return getNumberString(getIdentifier());
    }

    //
    //  Address string
    //
    public static String getAddressString(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("entity ID empty");
        }
        return identifier.address.toString();
    }
    public String getAddressString() {
        return getAddressString(getIdentifier());
    }

    //
    //  Name string
    //
    public static String getName(ID identifier) {
        String name;
        Profile profile = facebook.getProfile(identifier);
        if (profile != null) {
            name = profile.getName();
            if (name != null) {
                return name;
            }
        }
        name = identifier.name;
        if (name != null) {
            return name;
        }
        return identifier.toString();
    }
    public String getName() {
        return getName(getIdentifier());
    }

    //
    //  Profile
    //
    public static Profile getProfile(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("entity ID empty");
        }
        return facebook.getProfile(identifier);
    }
    public Profile getProfile() {
        return getProfile(getIdentifier());
    }
}
