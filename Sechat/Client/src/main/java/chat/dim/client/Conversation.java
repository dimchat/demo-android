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
package chat.dim.client;

import java.util.List;

import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.Group;
import chat.dim.mkm.entity.Entity;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.NetworkType;

public class Conversation {
    public static int PersonalChat = NetworkType.Main.value;
    public static int GroupChat = NetworkType.Group.value;

    private final Entity entity;
    public final ID identifier;
    public final int type;

    public ConversationDataSource dataSource = null;

    public Conversation(Entity entity) {
        super();
        this.entity = entity;
        this.identifier = entity.identifier;
        this.type = getType(entity);
    }

    private int getType(Entity entity) {
        NetworkType type = entity.getType();
        if (type.isGroup()) {
            return GroupChat;
        }
        return PersonalChat;
    }

    public String getName() {
        return entity.getName();
    }

    public String getTitle() {
        String name = entity.getName();
        NetworkType type = entity.getType();
        if (type.isGroup()) {
            Group group = (Group) entity;
            List<ID> members = group.getMembers();
            int count = (members == null) ? 0 : members.size();
            // Group: "yyy (123)"
            return name + " (" + count + ")";
        }
        // Person: "xxx"
        return name;
    }

    // interfaces for ConversationDataSource

    public int numberOfMessages() {
        return dataSource.numberOfMessages(this);
    }

    public InstantMessage messageAtIndex(int index) {
        return dataSource.messageAtIndex(index, this);
    }

    public boolean insertMessage(InstantMessage iMsg) {
        return dataSource.insertMessage(iMsg, this);
    }

    public boolean removeMessage(InstantMessage iMsg) {
        return dataSource.removeMessage(iMsg, this);
    }

    public boolean withdrawMessage(InstantMessage iMsg) {
        return dataSource.withdrawMessage(iMsg, this);
    }

    public boolean saveReceipt(InstantMessage receipt) {
        return dataSource.saveReceipt(receipt, this);
    }
}
