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
package chat.dim.common;

import java.util.ArrayList;
import java.util.List;

import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.threading.BackgroundThreads;

public abstract class Messenger extends chat.dim.Messenger {

    public Messenger()  {
        super();
        setCipherKeyDelegate(KeyStore.getInstance());
    }

    // check whether group info empty
    private boolean isEmpty(ID group) {
        chat.dim.Facebook facebook = getFacebook();
        List members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            return true;
        }
        ID owner = facebook.getOwner(group);
        return owner == null;
    }

    // check whether need to update group
    public boolean checkGroup(Content content, ID sender) {
        // Check if it is a group message, and whether the group members info needs update
        ID group = content.getGroup();
        if (group == null || ID.isBroadcast(group)) {
            // 1. personal message
            // 2. broadcast message
            return false;
        }
        // check meta for new group ID
        chat.dim.Facebook facebook = getFacebook();
        Meta meta = facebook.getMeta(group);
        if (meta == null) {
            // NOTICE: if meta for group not found,
            //         facebook should query it from DIM network automatically
            // TODO: insert the message to a temporary queue to wait meta
            //throw new NullPointerException("group meta not found: " + group);
            return true;
        }
        // query group info
        if (isEmpty(group)) {
            // NOTICE: if the group info not found, and this is not an 'invite' command
            //         query group info from the sender
            if (content instanceof InviteCommand || content instanceof ResetCommand) {
                // FIXME: can we trust this stranger?
                //        may be we should keep this members list temporary,
                //        and send 'query' to the owner immediately.
                // TODO: check whether the members list is a full list,
                //       it should contain the group owner(owner)
                return false;
            } else {
                return queryGroupInfo(group, sender);
            }
        } else if (facebook.containsMember(sender, group)
                || facebook.containsAssistant(sender, group)
                || facebook.isOwner(sender, group)) {
            // normal membership
            return false;
        } else {

            // if assistants exists, query them
            List<ID> assistants = facebook.getAssistants(group);
            List<ID> admins = new ArrayList<>(assistants);
            // if owner found, query it too
            ID owner = facebook.getOwner(group);
            if (owner != null && !admins.contains(owner)) {
                admins.add(owner);
            }
            return queryGroupInfo(group, admins);
        }
    }

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        Object reused = password.get("reused");
        if (reused != null) {
            ID receiver = iMsg.getReceiver();
            if (ID.isGroup(receiver)) {
                // reuse key for grouped message
                return null;
            }
            // remove before serialize key
            password.remove("reused");
        }
        byte[] data = super.serializeKey(password, iMsg);
        if (reused != null) {
            // put it back
            password.put("reused", reused);
        }
        return data;
    }

    //-------- Send

    @Override
    public boolean sendMessage(final InstantMessage iMsg, final Callback callback, final int priority) {
        BackgroundThreads.wait(new Runnable() {
            @Override
            public void run() {
                // Send message (secured + certified) to target station
                SecureMessage sMsg = getMessageProcessor().encryptMessage(iMsg);
                if (sMsg == null) {
                    // public key not found?
                    return ;
                    //throw new NullPointerException("failed to encrypt message: " + iMsg);
                }
                ReliableMessage rMsg = getMessageProcessor().signMessage(sMsg);
                if (rMsg == null) {
                    // TODO: set iMsg.state = error
                    throw new NullPointerException("failed to sign message: " + sMsg);
                }

                sendMessage(rMsg, callback, priority);
                // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting

                // save signature for receipt
                iMsg.put("signature", rMsg.get("signature"));

                saveMessage(iMsg);
            }
        });
        return true;
    }

    public boolean sendCommand(Command cmd, int priority) {
        return false;
    }

    public boolean queryMeta(ID identifier) {
        return false;
    }

    public boolean queryProfile(ID identifier) {
        return false;
    }

    public boolean queryGroupInfo(ID group, List<ID> members) {
        return false;
    }

    public boolean queryGroupInfo(ID group, ID member) {
        List<ID> array = new ArrayList<>();
        array.add(member);
        return queryGroupInfo(group, array);
    }
}
