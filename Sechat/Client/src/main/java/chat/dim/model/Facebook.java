/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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
package chat.dim.model;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.network.Downloader;

public class Facebook extends chat.dim.common.Facebook {
    private static final Facebook ourInstance = new Facebook();
    public static Facebook getInstance() { return ourInstance; }
    private Facebook() {
        super();
    }

    public String getAvatar(ID identifier) {
        Profile profile = getProfile(identifier);
        if (profile == null) {
            return null;
        }
        String url;
        if (profile instanceof UserProfile) {
            url = ((UserProfile) profile).getAvatar();
        } else {
            url = (String) profile.getProperty("avatar");
        }
        if (url == null) {
            return null;
        }
        Downloader downloader = Downloader.getInstance();
        return downloader.download(url);
    }

    //-------- EntityDataSource

    @Override
    public Meta getMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            // broadcast ID has not meta
            return null;
        }
        // try from database
        Meta meta = super.getMeta(identifier);
        if (meta != null) {
            return meta;
        }
        // query from DIM network
        Messenger messenger = Messenger.getInstance();
        messenger.queryMeta(identifier);
        return null;
    }

    @Override
    public Profile getProfile(ID identifier) {
        // try from database
        Profile profile = super.getProfile(identifier);
        if (profile != null) {
            return profile;
        }
        // query from DIM network
        Messenger messenger = Messenger.getInstance();
        messenger.queryProfile(identifier);
        return null;
    }

    @Override
    public List<ID> getAssistants(ID group) {
        List<ID> assistants = new ArrayList<>();
        // dev
        assistants.add(getID("assistant@2PpB6iscuBjA15oTjAsiswoX9qis5V3c1Dq"));
        // desktop.dim.chat
        assistants.add(getID("assistant@4WBSiDzg9cpZGPqFrQ4bHcq4U5z9QAQLHS"));
        return assistants;
        //return super.getAssistants(group);
    }
}
