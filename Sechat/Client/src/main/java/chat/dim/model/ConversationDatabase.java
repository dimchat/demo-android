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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Message;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.database.ConversationTable;
import chat.dim.database.MessageTable;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Command;
import chat.dim.utils.Times;

public class ConversationDatabase implements ConversationDataSource {
    private static final ConversationDatabase ourInstance = new ConversationDatabase();
    public static ConversationDatabase getInstance() { return ourInstance; }
    private ConversationDatabase() {
        super();
        AnyContentProcessor.facebook = Facebook.getInstance();
    }

    private ConversationTable conversationTable = new ConversationTable();
    private MessageTable messageTable = new MessageTable();

    public String getTimeString(Message msg) {
        Date time = msg.envelope.time;
        if (time == null) {
            return null;
        }
        return Times.getTimeString(time);
    }

    public String getContentText(Content content) {
        return AnyContentProcessor.getContentText(content);
    }

    public String getCommandText(Command cmd, ID sender) {
        return AnyContentProcessor.getCommandText(cmd, sender);
    }

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
        if (!messageTable.removeMessages(identifier)) {
            return false;
        }
        if (!conversationTable.removeConversation(identifier)) {
            return false;
        }
        postMessageUpdatedNotification(null, identifier);
        return true;
    }

    public boolean clearConversation(ID identifier) {
        if (!messageTable.clearMessages(identifier)) {
            return false;
        }
        postMessageUpdatedNotification(null, identifier);
        return true;
    }

    public void reloadConversations() {
        conversationTable.reloadConversations();
    }

    // messages

    public List<InstantMessage> messagesInConversation(Conversation chatBox) {
        return messageTable.messagesInConversation(chatBox.identifier);
    }

    @Override
    public int numberOfMessages(Conversation chatBox) {
        return messageTable.numberOfMessages(chatBox.identifier);
    }

    @Override
    public InstantMessage messageAtIndex(int index, Conversation chatBox) {
        return messageTable.messageAtIndex(index, chatBox.identifier);
    }

    private void postMessageUpdatedNotification(InstantMessage iMsg, ID identifier) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("ID", identifier);
        userInfo.put("msg", iMsg);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.MessageUpdated, this, userInfo);
    }

    @Override
    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.insertMessage(iMsg, chatBox.identifier);
        if (OK) {
            conversationTable.insertConversation(chatBox.identifier);
            postMessageUpdatedNotification(iMsg, chatBox.identifier);
        }
        return OK;
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.removeMessage(iMsg, chatBox.identifier);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox.identifier);
        }
        return OK;
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.withdrawMessage(iMsg, chatBox.identifier);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox.identifier);
        }
        return OK;
    }

    @Override
    public boolean saveReceipt(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.saveReceipt(iMsg, chatBox.identifier);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox.identifier);
        }
        return OK;
    }
}
