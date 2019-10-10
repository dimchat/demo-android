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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.common.Conversation;
import chat.dim.common.ConversationDataSource;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.notification.NotificationCenter;

public class ConversationDatabase implements ConversationDataSource {

    // constants
    public static final String MessageUpdated = "MessageUpdated";
    public static final String MessageCleaned = "MessageCleaned";

    private ConversationTable conversationTable = new ConversationTable();
    private MessageTable messageTable = new MessageTable();

    //-------- ConversationDataSource

    @Override
    public int numberOfConversations() {
        return conversationTable.numberOfConversations();
    }

    @Override
    public ID conversationAtIndex(int index) {
        return conversationTable.conversationAtIndex(index);
    }

    @Override
    public boolean removeConversationAtIndex(int index) {
        return conversationTable.removeConversationAtIndex(index);
    }

    @Override
    public boolean removeConversation(ID identifier) {
        return conversationTable.removeConversation(identifier);
    }

    // messages

    public List<InstantMessage> messagesInConversation(Conversation chatBox) {
        return messageTable.messagesInConversation(chatBox);
    }

    @Override
    public int numberOfMessages(Conversation chatBox) {
        return messageTable.numberOfMessages(chatBox);
    }

    @Override
    public InstantMessage messageAtIndex(int index, Conversation chatBox) {
        return messageTable.messageAtIndex(index, chatBox);
    }

    private void postMessageUpdatedNotification(InstantMessage iMsg, Conversation chatBox) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("ID", chatBox.identifier);
        userInfo.put("msg", iMsg);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(MessageUpdated, this, userInfo);
    }

    @Override
    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.insertMessage(iMsg, chatBox);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox);
        }
        return OK;
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.removeMessage(iMsg, chatBox);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox);
        }
        return OK;
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.withdrawMessage(iMsg, chatBox);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox);
        }
        return OK;
    }

    @Override
    public boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        boolean OK = messageTable.saveReceipt(receipt, chatBox);
        if (OK) {
            postMessageUpdatedNotification(receipt, chatBox);
        }
        return OK;
    }
}
