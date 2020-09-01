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

import chat.dim.Entity;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.User;

public final class Amanuensis {
    private static final Amanuensis ourInstance = new Amanuensis();
    public static Amanuensis getInstance() { return ourInstance; }
    private Amanuensis() {
        super();
        database = ConversationDatabase.getInstance();
    }

    public final ConversationDatabase database;

    private final Facebook facebook = Facebook.getInstance();

    // conversation factory
    public Conversation getConversation(ID identifier) {
        // create directly if we can find the entity
        Entity entity = null;
        if (identifier.isUser()) {
            entity = facebook.getUser(identifier);
        } else if (identifier.isGroup()) {
            entity = facebook.getGroup(identifier);
        }
        if (entity == null) {
            throw new NullPointerException("failed to create conversation:" + identifier);
        }
        Conversation chatBox = new Conversation(entity);
        chatBox.database = database;
        return chatBox;
    }

    private Conversation getConversation(InstantMessage iMsg) {
        // check receiver
        ID receiver = (ID) iMsg.getReceiver();
        if (receiver.isGroup()) {
            // group chat, get chat box with group ID
            return getConversation(receiver);
        }
        // check group
        ID group = (ID) iMsg.getContent().getGroup();
        if (group != null) {
            // group chat, get chat box with group ID
            return getConversation(group);
        }
        // personal chat, get chat box with contact ID
        ID sender = (ID) iMsg.getSender();
        User user = facebook.getCurrentUser();
        if (sender.equals(user.identifier)) {
            return getConversation(receiver);
        } else {
            return getConversation(sender);
        }
    }

    public boolean saveMessage(InstantMessage iMsg) {
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
