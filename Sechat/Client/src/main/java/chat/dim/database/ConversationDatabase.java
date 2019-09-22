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

import java.util.List;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.client.ConversationDataSource;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;

public class ConversationDatabase implements ConversationDataSource {
    private static final ConversationDatabase ourInstance = new ConversationDatabase();
    public static ConversationDatabase getInstance() { return ourInstance; }
    private ConversationDatabase() {
        // initialized delegate of clerk
        Amanuensis clerk = Amanuensis.getInstance();
        clerk.conversationDataSource = this;
    }

    public void reloadData(ID user) {
        // reload conversations for current user
        ConversationTable.reloadData(user);
    }

    //-------- ConversationDataSource

    @Override
    public int numberOfConversations() {
        return ConversationTable.numberOfConversations();
    }

    @Override
    public Conversation conversationAtIndex(int index) {
        return ConversationTable.conversationAtIndex(index);
    }

    @Override
    public ID removeConversationAtIndex(int index) {
        return ConversationTable.removeConversationAtIndex(index);
    }

    @Override
    public boolean removeConversation(Conversation chatBox) {
        return ConversationTable.removeConversation(chatBox);
    }

    // messages

    public List<InstantMessage> messagesInConversation(Conversation chatBox) {
        return MessageTable.messagesInConversation(chatBox);
    }

    @Override
    public int numberOfMessages(Conversation chatBox) {
        return MessageTable.numberOfMessages(chatBox);
    }

    @Override
    public InstantMessage messageAtIndex(int index, Conversation chatBox) {
        return MessageTable.messageAtIndex(index, chatBox);
    }

    @Override
    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        return MessageTable.insertMessage(iMsg, chatBox);
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        return MessageTable.removeMessage(iMsg, chatBox);
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        return MessageTable.withdrawMessage(iMsg, chatBox);
    }

    @Override
    public boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        return MessageTable.saveReceipt(receipt, chatBox);
    }
}
