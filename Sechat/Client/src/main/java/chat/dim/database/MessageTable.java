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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.model.Conversation;

public class MessageTable extends ExternalStorage {

    private Map<ID, List<InstantMessage>> chatHistory = new HashMap<>();

    // "/sdcard/chat.dim.sechat/dkd/{address}/messages.js"

    private static String getMsgFilePath(ID entity) {
        return root + "/dkd/" + entity.address + "/messages.js";
    }

    private List cacheMessages(Object array, ID entity) {
        if (!(array instanceof List)) {
            return null;
        }
        List list = (List) array;
        List<InstantMessage> messages = new ArrayList<>();
        for (Object msg : list) {
            messages.add(InstantMessage.getInstance(msg));
        }
        chatHistory.put(entity, messages);
        return messages;
    }

    private List loadMessages(ID entity) {
        String path = getMsgFilePath(entity);
        try {
            Object array = readJSON(path);
            return cacheMessages(array, entity);
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
    }

    private boolean saveMessages(ID entity) {
        List<InstantMessage> messages = chatHistory.get(entity);
        if (messages == null) {
            return false;
        }
        String path = getMsgFilePath(entity);
        try {
            return writeJSON(messages, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean removeMessages(ID entity) {
        String path = getMsgFilePath(entity);
        try {
            return delete(path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean clearMessages(ID entity) {
        List<InstantMessage> messages = new ArrayList<>();
        String path = getMsgFilePath(entity);
        try {
            return writeJSON(messages, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    //-------- messages

    public List<InstantMessage> messagesInConversation(Conversation chatBox) {
        List<InstantMessage> msgList = chatHistory.get(chatBox.identifier);
        if (msgList == null) {
            msgList = new ArrayList<>();
            List messages = loadMessages(chatBox.identifier);
            if (messages != null) {
                InstantMessage msg;
                for (Object item : messages) {
                    msg = InstantMessage.getInstance(item);
                    if (msg == null) {
                        throw new NullPointerException("message error: " + item);
                    }
                    msgList.add(msg);
                }
            }
            chatHistory.put(chatBox.identifier, msgList);
        }
        return msgList;
    }

    public int numberOfMessages(Conversation chatBox) {
        List<InstantMessage> msgList = messagesInConversation(chatBox);
        return msgList.size();
    }

    public InstantMessage messageAtIndex(int index, Conversation chatBox) {
        List<InstantMessage> msgList = messagesInConversation(chatBox);
        return msgList.get(index);
    }

    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        List<InstantMessage> msgList = messagesInConversation(chatBox);
        msgList.add(iMsg);
        return saveMessages(chatBox.identifier);
    }

    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        List<InstantMessage> msgList = messagesInConversation(chatBox);
        msgList.remove(iMsg);
        return saveMessages(chatBox.identifier);
    }

    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        // TODO: withdraw a message;
        return false;
    }

    public boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        // TODO: save receipt of instant message
        return false;
    }

    public boolean removeMessages(Conversation chatBox) {
        return removeMessages(chatBox.identifier);
    }

    public boolean clearMessages(Conversation chatBox) {
        return clearMessages(chatBox.identifier);
    }
}
