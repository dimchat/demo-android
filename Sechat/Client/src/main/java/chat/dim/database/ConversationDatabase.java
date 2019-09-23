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

import chat.dim.client.Conversation;
import chat.dim.client.ConversationDataSource;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;

public class ConversationDatabase implements ConversationDataSource {

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

    @Override
    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        return messageTable.insertMessage(iMsg, chatBox);
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        return messageTable.removeMessage(iMsg, chatBox);
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        return messageTable.withdrawMessage(iMsg, chatBox);
    }

    @Override
    public boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        return messageTable.saveReceipt(receipt, chatBox);
    }
}
