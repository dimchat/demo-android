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

import chat.dim.ID;
import chat.dim.InstantMessage;

public interface ConversationDataSource {

    //---- conversations

    /**
     *  Get how many chat boxes
     *
     * @return conversations count
     */
    int numberOfConversations();

    /**
     *  Get chat box info
     *
     * @param index - sorted index
     * @return conversation ID
     */
    ID conversationAtIndex(int index);

    /**
     *  Remove one chat box
     *
     * @param index - chat box index
     * @return false on error
     */
    boolean removeConversationAtIndex(int index);

    /**
     *  Remove the chat box
     *
     * @param identifier - conversation ID
     * @return false on error
     */
    boolean removeConversation(ID identifier);

    //-------- messages

    /**
     *  Get message count in this conversation for an entity
     *
     * @param chatBox - conversation instance
     * @return total count
     */
    int numberOfMessages(Conversation chatBox);

    /**
     *  Get unread message count in this conversation for an entity
     *
     * @param chatBox - conversation instance
     * @return unread count
     */
    int numberOfUnreadMessages(Conversation chatBox);

    /**
     *  Clear unread flag in this conversation for an entity
     *
     * @param chatBox - conversation instance
     * @return false on failed
     */
    boolean clearUnreadMessages(Conversation chatBox);

    /**
     *  Get message at index of this conversation
     *
     * @param index - start from 0, latest first
     * @param chatBox - conversation instance
     * @return instant message
     */
    InstantMessage messageAtIndex(int index, Conversation chatBox);

    /**
     *  Save the new message to local storage
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    boolean insertMessage(InstantMessage iMsg, Conversation chatBox);

    /**
     *  Delete the message
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    boolean removeMessage(InstantMessage iMsg, Conversation chatBox);

    /**
     *  Try to withdraw the message, maybe won't success
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox);

    /**
     *  Update message state with receipt
     *
     * @param iMsg - message with receipt content
     * @param chatBox - conversation
     * @return true while target message found
     */
    boolean saveReceipt(InstantMessage iMsg, Conversation chatBox);
}
