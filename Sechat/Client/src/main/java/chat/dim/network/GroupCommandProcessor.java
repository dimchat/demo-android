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
package chat.dim.network;

import java.util.ArrayList;
import java.util.List;

import chat.dim.common.Facebook;
import chat.dim.group.Polylogue;
import chat.dim.mkm.ID;
import chat.dim.mkm.NetworkType;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.GroupCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.utils.Log;

class GroupCommandProcessor {

    private Facebook facebook = Facebook.getInstance();

    boolean process(GroupCommand cmd, ID sender) {
        boolean OK;

        ID gid = facebook.getID(cmd.getGroup());
        if (gid.getType() == NetworkType.Polylogue) {
            Polylogue group = (Polylogue) facebook.getGroup(gid);
            if (cmd instanceof InviteCommand) {
                OK = processInvite((InviteCommand) cmd, sender, group);
            } else if (cmd instanceof ExpelCommand) {
                OK = processExpel((ExpelCommand) cmd, sender, group);
            } else if (cmd instanceof QuitCommand) {
                OK = processQuit((QuitCommand) cmd, sender, group);
            } else if (cmd instanceof ResetCommand) {
                OK = processReset(cmd, sender, group);
            } else if (cmd instanceof QueryCommand) {
                OK = processQuery((QueryCommand) cmd, sender, group);
            } else {
                throw new UnsupportedOperationException("unsupported polylogue command: " + cmd);
            }
        } else {
            throw new UnsupportedOperationException("unsupported group command: " + cmd);
        }

        return OK;
    }

    private boolean processInvite(InviteCommand cmd, ID commander, Polylogue group) {
        // 1. check permission
        ID founder = group.getFounder();
        List<ID> members = group.getMembers();
        if (founder == null && (members == null || members.size() == 0)) {
            // FIXME: group profile lost?
            // FIXME: how to avoid strangers impersonating group members?
        } else if (!facebook.existsMember(commander, group.identifier)) {
            Log.error(commander + " is not a member of polylogue: " + group + ", cannot invite");
            return false;
        }

        // 2. get member(s)
        List<ID> invitedList = new ArrayList<>();
        List array = cmd.members;
        if (array != null && array.size() > 0) {
            // replace item to ID objects
            for (Object item : array) {
                invitedList.add(facebook.getID(item));
            }
        }
        List<ID> oldMembers = group.getMembers();

        // 2.1. check founder for reset command
        if (facebook.isFounder(commander, group.identifier)) {
            for (ID item : invitedList) {
                if (facebook.isFounder(item, group.identifier)) {
                    // invite founder? it means this should be a 'reset' command
                    return processReset(cmd, commander, group);
                }
            }
        }

        // 2.2. check added members
        List<ID> addedList = new ArrayList<>();
        for (ID item : invitedList) {
            if (!oldMembers.contains(item)) {
                oldMembers.add(item);
                addedList.add(item);
            }
            // NOTE:
            //    the owner will receive the invite command sent by itself
            //    after it's already added these members to the group,
            //    just ignore this assert.
        }

        if (addedList.size() > 0) {
            cmd.put("added", addedList);
            Log.info("invite members to group: " + group + ", " + addedList);

            // 3. save new member list
            if (!facebook.saveMembers(oldMembers, group.identifier)) {
                Log.error("failed to save members of group: " + group);
                return false;
            }
        }

        return true;
    }

    private boolean processExpel(ExpelCommand cmd, ID commander, Polylogue group) {
        // 1. check permission
        if (!facebook.isFounder(commander, group.identifier)) {
            Log.error(commander + " is not the founder of polylogue: " + group + ", cannot expel");
            return false;
        }

        // 2. get member(s)
        List<ID> expelledList = new ArrayList<>();
        List array = cmd.members;
        if (array != null && array.size() > 0) {
            // replace item to ID objects
            for (Object item : array) {
                expelledList.add(facebook.getID(item));
            }
        }
        List<ID> oldMembers = group.getMembers();

        // 2.1. check removed member(s)
        List<ID> removedList = new ArrayList<>();
        for (ID item : expelledList) {
            if (oldMembers.contains(item)) {
                oldMembers.remove(item);
                removedList.add(item);
            }
            // NOTE:
            //    the owner will receive the expel command sent by itself
            //    after it's already removed these members from the group,
            //    just ignore this assert.
        }
        if (removedList.size() > 0) {
            cmd.put("removed", removedList);
            Log.error("expel members from group: " + group + ", " + removedList);

            // 3. save new member list
            if (!facebook.saveMembers(oldMembers, group.identifier)) {
                Log.error("failed to save members of group: " + group);
                return false;
            }
        }

        return true;
    }

    private boolean processQuit(QuitCommand cmd, ID commander, Polylogue group) {
        // 1. check permission
        if (facebook.isFounder(commander, group.identifier)) {
            Log.error(commander + " is the founder of polylogue: " + group + ", cannot quit");
            return false;
        }
        if (!facebook.existsMember(commander, group.identifier)) {
            Log.error(commander + " is not a member of polylogue: " + group + ", cannot quit");
            return false;
        }

        // 2. remove member
        if (!facebook.removeMember(commander, group.identifier)) {
            Log.error("failed to remove member " + commander + " from group: " + group);
            return false;
        }

        return true;
    }

    private boolean processReset(GroupCommand cmd, ID commander, Polylogue group) {
        // 1. check permission
        if (!facebook.isFounder(commander, group.identifier)) {
            Log.error(commander + " is not the founder of: " + group + ", cannot reset.");
            return false;
        }

        // 2. get member(s)
        List<ID> newMembers = new ArrayList<>();
        List array = cmd.members;
        if (array != null && array.size() > 0) {
            // replace item to ID objects
            for (Object item : array) {
                newMembers.add(facebook.getID(item));
            }
        }
        List<ID> oldMembers = group.getMembers();

        // 2.1. check removed member(s)
        List<ID> removedList = new ArrayList<>();
        for (ID item : oldMembers) {
            if (!newMembers.contains(item)) {
                removedList.add(item);
            }
        }

        // 2.2. check added member(s)
        List<ID> addedList = new ArrayList<>();
        for (ID item : newMembers) {
            if (!oldMembers.contains(item)) {
                addedList.add(item);
            }
        }

        if (addedList.size() > 0 || removedList.size() > 0) {
            cmd.put("added", addedList);
            cmd.put("removed", removedList);
            Log.info("reset group members: " + group + ",\n" + oldMembers + " ->\n" + newMembers);

            // 3. save new members list
            if (!facebook.saveMembers(newMembers, group.identifier)) {
                Log.error("failed to save members of group: " + group);
                return false;
            }
        }

        return true;
    }

    private boolean processQuery(QueryCommand cmd, ID commander, Polylogue group) {
        // 1. check permission
        if (!facebook.existsMember(commander, group.identifier)) {
            Log.error(commander + " is not a member of: " + group + ", cannot query.");
            return false;
        }

        // TODO: send group info to commander

        return true;
    }
}
