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
import java.util.Map;

import chat.dim.Envelope;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Message;
import chat.dim.User;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.database.MessageTable;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Command;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.utils.Times;

public final class ConversationDatabase {
    private static final ConversationDatabase ourInstance = new ConversationDatabase();
    public static ConversationDatabase getInstance() { return ourInstance; }
    private ConversationDatabase() {
        super();
        AnyContentProcessor.facebook = Facebook.getInstance();
    }

    public MessageTable messageTable = null;

    public String getTimeString(Message msg) {
        Date time = msg.getTime();
        if (time == null) {
            return null;
        }
        return Times.getTimeString(time);
    }

    public String getContentText(chat.dim.Content content) {
        return AnyContentProcessor.getContentText(content);
    }

    public String getCommandText(Command cmd, ID sender) {
        return AnyContentProcessor.getCommandText(cmd, sender);
    }

    //-------- ConversationDataSource

    public int numberOfConversations() {
        return messageTable.numberOfConversations();
    }

    public ID conversationAtIndex(int index) {
        return messageTable.conversationAtIndex(index);
    }

    public boolean removeConversationAtIndex(int index) {
        return messageTable.removeConversationAtIndex(index);
    }

    public boolean removeConversation(ID identifier) {
        if (!messageTable.removeConversation(identifier)) {
            return false;
        }
        if (!messageTable.removeConversation(identifier)) {
            return false;
        }
        postMessageUpdatedNotification(null, identifier);
        return true;
    }

    public boolean clearConversation(ID identifier) {
        if (!messageTable.removeConversation(identifier)) {
            return false;
        }
        postMessageUpdatedNotification(null, identifier);
        return true;
    }

    // messages

    public int numberOfMessages(Conversation chatBox) {
        return messageTable.numberOfMessages(chatBox.identifier);
    }

    public int numberOfUnreadMessages(Conversation chatBox) {
        return messageTable.numberOfUnreadMessages(chatBox.identifier);
    }

    public boolean clearUnreadMessages(Conversation chatBox) {
        return messageTable.clearUnreadMessages(chatBox.identifier);
    }

    public InstantMessage lastMessage(Conversation chatBox) {
        return messageTable.lastMessage(chatBox.identifier);
    }

    public InstantMessage lastReceivedMessage() {
        User user = Facebook.getInstance().getCurrentUser();
        if (user == null) {
            return null;
        }
        return messageTable.lastReceivedMessage(user.identifier);
    }

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

    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.insertMessage(iMsg, chatBox.identifier);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox.identifier);
        }
        return OK;
    }

    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.removeMessage(iMsg, chatBox.identifier);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox.identifier);
        }
        return OK;
    }

    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.withdrawMessage(iMsg, chatBox.identifier);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox.identifier);
        }
        return OK;
    }

    public boolean saveReceipt(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.saveReceipt(iMsg, chatBox.identifier);
        if (OK) {
            ID entity = chatBox.identifier;
            // FIXME: check for origin conversation
            if (entity.isUser()) {
                ReceiptCommand receipt = (ReceiptCommand) iMsg.getContent();
                Envelope<ID> env = receipt.getEnvelope();
                if (env != null) {
                    ID sender = env.getSender();
                    if (sender != null && sender.equals(iMsg.getReceiver())) {
                        entity = env.getReceiver();
                    }
                }
            }
            postMessageUpdatedNotification(iMsg, entity);
        }
        return OK;
    }
}
