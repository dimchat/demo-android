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

import java.util.ArrayList;
import java.util.List;

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.port.Departure;
import chat.dim.protocol.Command;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QuitCommand;

/**
 *  This is for sending group message, or managing group members
 */
public final class GroupManager {

    private final ID group;

    public GroupManager(ID group) {
        this.group = group;
    }

    private static Messenger getMessenger() {
        return Messenger.getInstance();
    }
    private static Facebook getFacebook() {
        return getMessenger().getFacebook();
    }

    // send command to current station
    private static void sendGroupCommand(Command cmd) {
        getMessenger().sendCommand(cmd, Departure.Priority.NORMAL.value);
    }
    private static void sendGroupCommand(Command cmd, ID receiver) {
        getMessenger().sendContent(null, receiver, cmd, null, Departure.Priority.NORMAL.value);
    }
    private static void sendGroupCommand(Command cmd, List<ID> members) {
        if (members == null) {
            return;
        }
        for (ID receiver : members) {
            sendGroupCommand(cmd, receiver);
        }
    }

    /**
     *  Invite new members to this group
     *  (only existed member/assistant can do this)
     *
     * @param newMembers - new members ID list
     * @return true on success
     */
    public boolean invite(List<ID> newMembers) {
        Facebook facebook = getFacebook();
        List<ID> bots = facebook.getAssistants(group);
        List<ID> members = facebook.getMembers(group);
        if (members == null) {
            members = new ArrayList<>();
        }
        int count = members.size();

        // 0. build 'meta/document' command
        Meta meta = facebook.getMeta(group);
        if (meta == null) {
            throw new NullPointerException("failed to get meta for group: " + group);
        }
        Document doc = facebook.getDocument(group, "*");
        Command cmd;
        if (doc == null) {
            // empty document
            cmd = new MetaCommand(group, meta);
        } else {
            cmd = new DocumentCommand(group, meta, doc);
        }
        sendGroupCommand(cmd);                  // to current station
        sendGroupCommand(cmd, bots);            // to group assistants
        if (count <= 2) { // new group?
            // 1. send 'meta/document' to station and bots
            // 2. update local storage
            members = addMembers(newMembers, group);
            sendGroupCommand(cmd, members);     // to all members
            // 3. send 'invite' command with all members to all members
            cmd = new InviteCommand(group, members);
            sendGroupCommand(cmd, bots);        // to group assistants
            sendGroupCommand(cmd, members);     // to all members
        } else {
            // 1. send 'meta/document' to station, bots and all members
            //sendGroupCommand(cmd, members);     // to old members
            sendGroupCommand(cmd, newMembers);  // to new members
            // 2. send 'invite' command with new members to old members
            cmd = new InviteCommand(group, newMembers);
            sendGroupCommand(cmd, bots);        // to group assistants
            sendGroupCommand(cmd, members);     // to old members
            // 3. update local storage
            members = addMembers(newMembers, group);
            // 4. send 'invite' command with all members to new members
            cmd = new InviteCommand(group, members);
            sendGroupCommand(cmd, newMembers);  // to new members
        }

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
        Facebook facebook = getFacebook();
        ID owner = facebook.getOwner(group);
        List<ID> bots = facebook.getAssistants(group);
        List<ID> members = facebook.getMembers(group);
        if (members == null) {
            members = new ArrayList<>();
        }

        // 0. check members list
        for (ID assistant : bots) {
            if (outMembers.contains(assistant)) {
                throw new RuntimeException("Cannot expel group assistant: " + assistant);
            }
        }
        if (outMembers.contains(owner)) {
            throw new RuntimeException("Cannot expel group owner: " + owner);
        }

        // 1. send 'expel' command to all members
        Command cmd = new ExpelCommand(group, outMembers);
        sendGroupCommand(cmd, bots);        // to assistants
        sendGroupCommand(cmd, members);     // to existed members
        if (owner != null && !members.contains(owner)) {
            sendGroupCommand(cmd, owner);   // to owner
        }

        // 2. update local storage
        return removeMembers(outMembers, group);
    }

    /**
     *  Quit from this group
     *  (only group member can do this)
     *
     * @return true on success
     */
    public boolean quit() {
        Facebook facebook = getFacebook();
        User user = facebook.getCurrentUser();
        if (user == null) {
            throw new NullPointerException("failed to get current user");
        }

        ID owner = facebook.getOwner(group);
        List<ID> bots = facebook.getAssistants(group);
        List<ID> members = facebook.getMembers(group);
        if (members == null) {
            members = new ArrayList<>();
        }

        // 0. check members list
        if (bots.contains(user.identifier)) {
            throw new RuntimeException("Group assistant cannot quit: " + user.identifier);
        }
        if (user.identifier.equals(owner)) {
            throw new RuntimeException("Group owner cannot quit: " + owner);
        }

        // 1. send 'quit' command to all members
        Command cmd = new QuitCommand(group);
        sendGroupCommand(cmd, bots);        // to assistants
        sendGroupCommand(cmd, members);     // to existed members
        if (owner != null && !members.contains(owner)) {
            sendGroupCommand(cmd, owner);   // to owner
        }

        // 2. update local storage
        return facebook.removeGroup(group);
    }

    /**
     *  Query group info
     *
     * @return false on error
     */
    public boolean query() {
        List<ID> assistants = getFacebook().getAssistants(group);
        assert assistants != null : "failed to get assistants for group: " + group;
        return getMessenger().queryGroupInfo(group, assistants);
    }

    //-------- local storage

    private static List<ID> addMembers(List<ID> newMembers, ID group) {
        List<ID> members = getFacebook().getMembers(group);
        assert members != null : "failed to get members for group: " + group;
        int count = 0;
        for (ID member : newMembers) {
            if (members.contains(member)) {
                continue;
            }
            members.add(member);
            ++count;
        }
        if (count > 0) {
            getFacebook().saveMembers(members, group);
        }
        return members;
    }
    private static boolean removeMembers(List<ID> outMembers, ID group) {
        List<ID> members = getFacebook().getMembers(group);
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
        return getFacebook().saveMembers(members, group);
    }
}
