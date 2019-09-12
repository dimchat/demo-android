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

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.mkm.ID;

public class ConversationTable extends ExternalStorage {

    private static List<ID> conversationList = new ArrayList<>();

    // "/sdcard/chat.dim.sechat/dkd/*"

    private static String getPath(ID user) {
        return root + "/dkd";
    }

    /**
     *  Refresh conversation list for current user
     *
     * @param user - user ID
     */
    static void reloadData(ID user) {
        // scan 'message.js' in sub dirs of 'dkd'
        String path = getPath(user);
        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        File[] items = dir.listFiles();
        if (items == null || items.length == 0) {
            return;
        }
        for (File file : items) {
            if (!file.isDirectory()) {
                continue;
            }
            if (!(new File(file.getPath(), "messages.js")).exists()) {
                continue;
            }
            conversationList.add(ID.getInstance(file.getPath()));
        }
        // sort with last message's time
        sortConversations();
    }

    private static void sortConversations() {
        // TODO: get last time of each conversations and sort
    }

    //---- conversations

    public static int numberOfConversations() {
        return conversationList.size();
    }

    public static Conversation conversationAtIndex(int index) {
        ID identifier = conversationList.get(index);
        Amanuensis clerk = Amanuensis.getInstance();
        return clerk.getConversation(identifier);
    }

    public static ID removeConversationAtIndex(int index) {
        return conversationList.remove(index);
    }

    public static boolean removeConversation(Conversation chatBox) {
        ID identifier = chatBox.identifier;
        return conversationList.remove(identifier);
    }
}
