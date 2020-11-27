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
package chat.dim;

import java.util.List;

import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.protocol.Command;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.stargate.StarShip;

/**
 *  This is for sending group message, or managing group members
 */
public class GroupManager {

    private static Facebook facebook = Facebook.getInstance();
    private static Messenger messenger = Messenger.getInstance();

    private final ID group;

    public GroupManager(ID group) {
        this.group = group;
    }

    /**
     *  Invite new members to this group
     *  (only existed member/assistant can do this)
     *
     * @param newMembers - new members ID list
     * @return true on success
     */
    public boolean invite(List<ID> newMembers) {
        Command cmd;
        // 1. send group info to all
        Meta meta = facebook.getMeta(group);
        assert meta != null : "failed to get meta for group: " + group;
        Document profile = facebook.getDocument(group, Document.BULLETIN);
        if (facebook.isEmpty(profile)) {
            // empty profile
            cmd = new MetaCommand(group, meta);
        } else {
            cmd = new DocumentCommand(group, meta, profile);
        }
        messenger.sendCommand(cmd, StarShip.NORMAL);         // to current station
        broadcastGroupCommand(cmd, group);  // to existed relationship
        sendGroupCommand(cmd, newMembers);  // to new members

        // 2. send 'INVITE' command with new members to existed relationship
        cmd = new InviteCommand(group, newMembers);
        broadcastGroupCommand(cmd, group);

        // 3. update local storage
        if (!addMembers(newMembers, group)) {
            return false;
        }
        List<ID> members = facebook.getMembers(group);

        // 4. send 'INVITE' command with all members to new members
        cmd = new InviteCommand(group, members);
        sendGroupCommand(cmd, newMembers);
        return true;
    }

    /**
     *  Expel members from this group
     *  (only group owner/assistant can do this)
     *
     * @param outMembers - existed member ID list
     * @return true on success
     */
    public boolean expel(List<ID> outMembers) {
        ID owner = facebook.getOwner(group);
        List<ID> assistants = facebook.getAssistants(group);
        List<ID> members = facebook.getMembers(group);
        assert owner != null : "failed to get owner of group: " + group;
        assert assistants != null : "failed to get assistants for group: " + group;
        assert members != null : "failed to get members of group: " + group;

        // 0. check members list
        for (ID ass : assistants) {
            if (outMembers.contains(ass)) {
                throw new RuntimeException("Cannot expel group assistant: " + ass);
            }
        }
        if (outMembers.contains(owner)) {
            throw new RuntimeException("Cannot expel group owner: " + owner);
        }

        // 1. update local storage
        if (!removeMembers(outMembers, group)) {
            return false;
        }

        // 2. send 'EXPEL' command to existed relationship
        Command cmd = new ExpelCommand(group, outMembers);
        broadcastGroupCommand(cmd, group);
        return true;
    }

    /**
     *  Quit from this group
     *  (only group member can do this)
     *
     * @return true on success
     */
    public boolean quit() {
        User user = facebook.getCurrentUser();
        if (user == null) {
            throw new NullPointerException("failed to get current user");
        }
        // 0. check members list
        ID owner = facebook.getOwner(group);
        if (user.identifier.equals(owner)) {
            throw new RuntimeException("Group owner cannot quit: " + owner);
        }

        // 1. send 'quit' command to all members
        Command cmd = new QuitCommand(group);
        broadcastGroupCommand(cmd, group);

        // 2. update local storage
        return facebook.removeGroup(group);
    }

    /**
     *  Query group info
     *
     * @return false on error
     */
    public boolean query() {
        List<ID> assistants = facebook.getAssistants(group);
        assert assistants != null : "failed to get assistants for group: " + group;
        return messenger.queryGroupInfo(group, assistants);
    }

    //-------- local storage

    private static boolean addMembers(List<ID> newMembers, ID group) {
        List<ID> members = facebook.getMembers(group);
        assert members != null : "failed to get members for group: " + group;
        int count = 0;
        for (ID member : newMembers) {
            if (members.contains(member)) {
                continue;
            }
            members.add(member);
            ++count;
        }
        if (count == 0) {
            return false;
        }
        return facebook.saveMembers(members, group);
    }
    private static boolean removeMembers(List<ID> outMembers, ID group) {
        List<ID> members = facebook.getMembers(group);
        assert members != null : "failed to get members for group: " + group;
        int count = 0;
        for (ID member : outMembers) {
            if (!members.contains(member)) {
                continue;
            }
            members.remove(member);
            ++count;
        }
        if (count == 0) {
            return false;
        }
        return facebook.saveMembers(members, group);
    }

    private static void broadcastGroupCommand(Command cmd, ID group) {
        // broadcast to assistants
        List<ID> assistants = facebook.getAssistants(group);
        sendGroupCommand(cmd, assistants);
        // broadcast to all members
        List<ID> members = facebook.getMembers(group);
        sendGroupCommand(cmd, members);
        // send to owner if not in member list
        ID owner = facebook.getOwner(group);
        if (owner != null && !members.contains(owner)) {
            messenger.sendContent(cmd, owner, null, StarShip.NORMAL);
        }
    }
    private static void sendGroupCommand(Command cmd, List<ID> members) {
        if (members != null) {
            for (ID receiver : members) {
                messenger.sendContent(cmd, receiver, null, StarShip.NORMAL);
            }
        }
    }
}
