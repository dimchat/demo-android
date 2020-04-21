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
package chat.dim.database;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.filesys.ExternalStorage;
import chat.dim.model.Facebook;
import chat.dim.utils.Log;

public class ConversationTable extends ExternalStorage {

    private List<ID> conversationList = null;

    // "/sdcard/chat.dim.sechat/dkd/*"
    private static String getMsgPath() {
        return root + separator + "dkd";
    }

    private boolean scanConversations() {
        assert conversationList == null : "conversations not found";
        conversationList = new ArrayList<>();
        // scan 'message.js' in sub dirs of 'dkd'
        String path = getMsgPath();
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return false;
        }
        File[] items = dir.listFiles();
        if (items == null || items.length == 0) {
            return false;
        }
        Facebook facebook = Facebook.getInstance();
        ID identifier;
        for (File file : items) {
            if (!file.isDirectory()) {
                continue;
            }
            if (!(new File(file.getPath(), "messages.js")).exists()) {
                continue;
            }
            identifier = ID.getInstance(file.getName());
            identifier = facebook.getID(identifier.address);
            if (identifier == null) {
                //throw new NullPointerException("failed to get ID with name: " + file.getName());
                Log.error("failed to get ID with name: " + file.getName());
                // FIXME: meta not found?
                continue;
            }
            conversationList.add(identifier);
        }
        // sort with last message's time
        sortConversations();
        return true;
    }

    private void sortConversations() {
        // TODO: get last time of each conversations and sort
    }

    //---- conversations

    public int numberOfConversations() {
        if (conversationList == null && ! scanConversations()) {
            return 0;
        }
        return conversationList.size();
    }

    public ID conversationAtIndex(int index) {
        return conversationList.get(index);
    }

    public boolean removeConversationAtIndex(int index) {
        ID identifier = conversationList.get(index);
        return removeConversation(identifier);
    }

    public boolean removeConversation(ID identifier) {
        // TODO: remove '{address}/messages.js'
        return conversationList.remove(identifier);
    }
}
