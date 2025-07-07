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

import java.util.List;

import chat.dim.CommonFacebook;
import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.dbi.AccountDBI;
import chat.dim.mkm.Entity;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.threading.BackgroundThreads;

public class EntityViewModel extends ViewModel {

    public static SharedFacebook getFacebook() {
        GlobalVariable shared = GlobalVariable.getInstance();
        return shared.facebook;
    }

    public static List<ID> getLocalUsers() {
        CommonFacebook facebook = getFacebook();
        AccountDBI db = facebook.getDatabase();
        return db.getLocalUsers();
    }

    private Entity entity = null;

    protected void setEntity(Entity entity) {
        this.entity = entity;
    }
    protected Entity getEntity() {
        return entity;
    }

    //
    //  create entity by ID
    //
    public void setIdentifier(ID identifier) {
        if (identifier.isGroup()) {
            setEntity(getFacebook().getGroup(identifier));
        } else if (identifier.isUser()) {
            setEntity(getFacebook().getUser(identifier));
        } else {
            throw new NullPointerException("ID error: " + identifier);
        }
    }
    public ID getIdentifier() {
        Entity entity = getEntity();
        if (entity == null) {
            return null;
        }
        return entity.getIdentifier();
    }

    public String getAddressString() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        return identifier.getAddress().toString();
    }

    public String getName() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        // get name from document
        return getFacebook().getName(identifier);
    }

    public Document getDocument(String type) {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        return getFacebook().getDocument(identifier, type);
    }

    public void refreshDocument() {
        BackgroundThreads.wait(() -> getDocument("*"));
    }
}
