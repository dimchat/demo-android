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

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.mkm.Entity;
import chat.dim.mkm.User;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReceiptCommand;

public final class Amanuensis {

    private static final Amanuensis ourInstance = new Amanuensis();
    public static Amanuensis getInstance() { return ourInstance; }
    private Amanuensis() {
        super();
    }

    // conversation factory
    public Conversation getConversation(ID identifier) {
        Facebook facebook = Messenger.getInstance().getFacebook();
        // create directly if we can find the entity
        Entity entity = null;
        if (identifier.isUser()) {
            entity = facebook.getUser(identifier);
        } else if (identifier.isGroup()) {
            entity = facebook.getGroup(identifier);
        }
        if (entity == null) {
            //throw new NullPointerException("failed to create conversation:" + identifier);
            return null;
        }
        Conversation chatBox = new Conversation(entity);
        chatBox.database = ConversationDatabase.getInstance();
        return chatBox;
    }

    private Conversation getConversation(InstantMessage iMsg) {
        // check receiver
        ID receiver = iMsg.getReceiver();
        if (receiver.isGroup()) {
            // group chat, get chat box with group ID
            return getConversation(receiver);
        }
        // check group
        ID group = iMsg.getGroup();
        if (group != null) {
            // group chat, get chat box with group ID
            return getConversation(group);
        }
        // personal chat, get chat box with contact ID
        Facebook facebook = Messenger.getInstance().getFacebook();
        ID sender = iMsg.getSender();
        User user = facebook.getCurrentUser();
        if (user.getIdentifier().equals(sender)) {
            return getConversation(receiver);
        } else {
            return getConversation(sender);
        }
    }

    public boolean saveMessage(InstantMessage iMsg) {
        if (iMsg.getContent() instanceof ReceiptCommand) {
            // it's a receipt
            return saveReceipt(iMsg);
        }
        Conversation chatBox = getConversation(iMsg);
        if (chatBox == null) {
            return false;
        }
        return chatBox.insertMessage(iMsg);
    }

    public boolean saveReceipt(InstantMessage iMsg) {
        Conversation chatBox = getConversation(iMsg);
        if (chatBox == null) {
            return false;
        }
        return chatBox.saveReceipt(iMsg);
    }
}
