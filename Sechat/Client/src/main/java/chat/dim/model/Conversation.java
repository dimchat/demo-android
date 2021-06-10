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
import java.util.List;

import chat.dim.Entity;
import chat.dim.Group;
import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.NetworkType;

public class Conversation {
    public static byte PersonalChat = NetworkType.MAIN.value;
    public static byte GroupChat = NetworkType.GROUP.value;

    private final Entity entity;
    public final ID identifier;
    public final byte type;

    public ConversationDatabase database = ConversationDatabase.getInstance();

    public Conversation(Entity entity) {
        super();
        this.entity = entity;
        this.identifier = entity.identifier;
        this.type = getType(entity);
    }

    private byte getType(Entity entity) {
        ID identifier = entity.identifier;
        if (identifier.isGroup()) {
            return GroupChat;
        }
        return PersonalChat;
    }

    public String getTitle() {
        Facebook facebook = Messenger.getInstance().getFacebook();
        String name = facebook.getName(entity.identifier);
        ID identifier = entity.identifier;
        if (identifier.isGroup()) {
            Group group = (Group) entity;
            List<ID> members = group.getMembers();
            int count = (members == null) ? 0 : members.size();
            if (count == 0) {
                return name + " (...)";
            }
            // Group: "yyy (123)"
            return name + " (" + count + ")";
        }
        // Person: "xxx"
        return name;
    }

    public Date getLastTime() {
        Date time = null;
        InstantMessage iMsg = getLastMessage();
        if (iMsg != null) {
            time = iMsg.getTime();
        }
        if (time == null) {
            time = new Date(0);
        }
        return time;
    }

    public InstantMessage getLastMessage() {
        return database.lastMessage(this);
    }

    public InstantMessage getLastVisibleMessage() {
        // return database.lastMessage(this);
        int count = numberOfMessages();
        InstantMessage iMsg;
        int msgType;
        for (int index = count - 1; index >= 0; --index) {
            iMsg = messageAtIndex(index);
            if (iMsg == null) {
                continue;
            }
            // FIXME: here will throw a NullPointerException
            msgType = iMsg.getType();
            if (msgType == ContentType.TEXT.value ||
                    msgType == ContentType.FILE.value ||
                    msgType == ContentType.IMAGE.value ||
                    msgType == ContentType.AUDIO.value ||
                    msgType == ContentType.VIDEO.value ||
                    msgType == ContentType.PAGE.value ||
                    msgType == ContentType.MONEY.value ||
                    msgType == ContentType.TRANSFER.value) {
                // got it
                return iMsg;
            }
        }
        return null;
    }

    // interfaces for ConversationDataSource

    public int numberOfMessages() {
        return database.numberOfMessages(this);
    }

    public int numberOfUnreadMessages() {
        return database.numberOfUnreadMessages(this);
    }

    public InstantMessage messageAtIndex(int index) {
        return database.messageAtIndex(index, this);
    }

    public boolean insertMessage(InstantMessage iMsg) {
        return database.insertMessage(iMsg, this);
    }

    public boolean removeMessage(InstantMessage iMsg) {
        return database.removeMessage(iMsg, this);
    }

    public boolean withdrawMessage(InstantMessage iMsg) {
        return database.withdrawMessage(iMsg, this);
    }

    public boolean saveReceipt(InstantMessage iMsg) {
        return database.saveReceipt(iMsg, this);
    }
}
