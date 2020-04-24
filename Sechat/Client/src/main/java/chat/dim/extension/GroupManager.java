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
package chat.dim.extension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.protocol.Command;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ProfileCommand;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QuitCommand;

/**
 *  This is for sending group message, or managing group members
 */
public class GroupManager {

    private final ID group;

    public GroupManager(ID group) {
        this.group = group;
    }

    /**
     *  Send message content to this group
     *  (only existed member can do this)
     *
     * @param content - message content
     * @return true on success
     */
    public boolean send(Content content) {
        Messenger messenger = Messenger.getInstance();
        Facebook facebook = Facebook.getInstance();
        // check group ID
        Object gid = content.getGroup();
        if (gid == null) {
            content.setGroup(group);
        } else {
            assert group.equals(gid) : "group ID not match: " + group + ", " + content;
        }
        // check members
        List<ID> members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            // get group assistant
            List<ID> assistants = facebook.getAssistants(group);
            assert assistants != null : "failed to get assistants for group: " + group;
            if (assistants.size() == 0) {
                throw new NullPointerException("group assistant not found: " + group);
            }
            // querying assistants for group info
            messenger.queryGroupInfo(group, assistants);
            return false;
        }
        // let group assistant to split and deliver this message to all members
        return messenger.sendContent(content, group, null);
    }

    private boolean sendGroupCommand(Command cmd, List<ID> members) {
        Messenger messenger = Messenger.getInstance();
        boolean ok = true;
        for (ID receiver : members) {
            if (!messenger.sendContent(cmd, receiver, null)) {
                ok = false;
            }
        }
        return ok;
    }
    private boolean sendGroupCommand(Command cmd, ID receiver) {
        Messenger messenger = Messenger.getInstance();
        return messenger.sendContent(cmd, receiver, null);
    }

    /**
     *  Invite new members to this group
     *  (only existed member/assistant can do this)
     *
     * @param newMembers - new members ID list
     * @return true on success
     */
    public boolean invite(List<ID> newMembers) {
        Facebook facebook = Facebook.getInstance();

        Command cmd;
        // 0. send 'meta/profile' command to new members
        Meta meta = facebook.getMeta(group);
        assert meta != null : "failed to get meta for group: " + group;
        Profile profile = facebook.getProfile(group);
        if (profile != null) {
            Set<String> names = profile.propertyNames();
            if (names == null || names.size() == 0) {
                // empty profile
                profile = null;
            }
        }
        if (profile == null) {
            cmd = new MetaCommand(group, meta);
        } else {
            cmd = new ProfileCommand(group, meta, profile);
        }
        sendGroupCommand(cmd, newMembers);

        ID owner = facebook.getOwner(group);
        List<ID> assistants = facebook.getAssistants(group);
        List<ID> members;
        assert assistants != null : "failed to get assistants for group: " + group;

        // 1. send 'invite' command with new members to existed members
        cmd = new InviteCommand(group, newMembers);
        // 1.1. send to existed members
        members = facebook.getMembers(group);
        sendGroupCommand(cmd, members);
        // 1.2. send to assistants
        sendGroupCommand(cmd, assistants);
        // 1.3. send to owner
        if (owner != null && !members.contains(owner)) {
            sendGroupCommand(cmd, owner);
        }

        // 2. update local storage
        addMembers(newMembers);

        // 3. send 'invite' with all members command to new members
        members = facebook.getMembers(group);
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
     * @throws IllegalAccessException on permission error
     */
    public boolean expel(List<ID> outMembers) throws IllegalAccessException {
        Facebook facebook = Facebook.getInstance();

        ID owner = facebook.getOwner(group);
        List<ID> assistants = facebook.getAssistants(group);
        List<ID> members = facebook.getMembers(group);
        assert owner != null : "failed to get owner of group: " + group;
        assert assistants != null : "failed to get assistants for group: " + group;
        assert members != null : "failed to get members of group: " + group;

        // 0. check members list
        for (ID ass : assistants) {
            if (outMembers.contains(ass)) {
                throw new IllegalAccessException("Cannot expel group assistant: " + ass);
            }
        }
        if (outMembers.contains(owner)) {
            throw new IllegalAccessException("Cannot expel group owner: " + owner);
        }

        // 1. send 'expel' command to all members
        Command cmd = new ExpelCommand(group, outMembers);
        // 1.1. send to existed members
        sendGroupCommand(cmd, members);
        // 1.2. send to assistants
        sendGroupCommand(cmd, assistants);
        // 1.3. send to owner
        if (!members.contains(owner)) {
            sendGroupCommand(cmd, owner);
        }

        // 2. update local storage
        return removeMembers(outMembers);
    }

    /**
     *  Quit from this group
     *  (only group member can do this)
     *
     * @param me - my ID
     * @return true on success
     */
    public boolean quit(ID me) throws IllegalAccessException {
        Facebook facebook = Facebook.getInstance();

        ID owner = facebook.getOwner(group);
        List<ID> assistants = facebook.getAssistants(group);
        List<ID> members = facebook.getMembers(group);
        assert owner != null : "failed to get owner of group: " + group;
        assert assistants != null : "failed to get assistants for group: " + group;
        assert members != null : "failed to get members of group: " + group;

        // 0. check members list
        for (ID ass : assistants) {
            if (me.equals(ass)) {
                throw new IllegalAccessException("Group assistant cannot quit: " + ass);
            }
        }
        if (me.equals(owner)) {
            throw new IllegalAccessException("Group owner cannot quit: " + owner);
        }

        // 1. send 'quit' command to all members
        Command cmd = new QuitCommand(group);
        // 1.1. send to existed members
        sendGroupCommand(cmd, members);
        // 1.2. send to assistants
        sendGroupCommand(cmd, assistants);
        // 1.3. send to owner
        if (!members.contains(owner)) {
            sendGroupCommand(cmd, owner);
        }

        // 2. update local storage
        return removeMember(me);
    }

    //-------- local storage

    public boolean addMembers(List<ID> newMembers) {
        Facebook facebook = Facebook.getInstance();
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
    public boolean removeMembers(List<ID> outMembers) {
        Facebook facebook = Facebook.getInstance();
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

    public boolean addMember(ID member) {
        Facebook facebook = Facebook.getInstance();
        return facebook.addMember(member, group);
    }
    public boolean removeMember(ID member) {
        Facebook facebook = Facebook.getInstance();
        return facebook.removeMember(member, group);
    }

    /**
     *  Test case
     *
     * @param args - command arguments
     */
    public static void main(String[] args) {
        Facebook facebook = Facebook.getInstance();

        ID group = facebook.getID("Group-Naruto@7ThVZeDuQAdG3eSDF6NeFjMDPjKN5SbrnM");
        ID hulk = facebook.getID("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");
        ID moki = facebook.getID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");

        GroupManager gm = new GroupManager(group);

        // invite members
        List<ID> members = new ArrayList<>();
        members.add(hulk);
        members.add(moki);
        gm.invite(members);

        // send message content
        Content content = new TextContent("Hello world!");
        gm.send(content);
    }
}
