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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Content;
import chat.dim.Envelope;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Meta;
import chat.dim.ReliableMessage;
import chat.dim.SecureMessage;
import chat.dim.core.CipherKeyDelegate;
import chat.dim.core.EntityDelegate;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.cpu.BlockCommandProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.MuteCommandProcessor;
import chat.dim.cpu.ReceiptCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.digest.SHA256;
import chat.dim.format.Base64;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.ResetCommand;

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
    private boolean checkGroup(Content content, ID sender) {
        // Check if it is a group message, and whether the group members info needs update
        chat.dim.Facebook facebook = getFacebook();
        ID group = facebook.getID(content.getGroup());
        if (group == null || group.isBroadcast()) {
            // 1. personal message
            // 2. broadcast message
            return false;
        }
        // check meta for new group ID
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
        } else if (facebook.existsMember(sender, group)
                || facebook.existsAssistant(sender, group)
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

    //-------- Serialization

    @Override
    public byte[] serializeMessage(ReliableMessage rMsg) {
        attachKeyDigest(rMsg);
        return super.serializeMessage(rMsg);
    }

    private void attachKeyDigest(ReliableMessage rMsg) {
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(this);
        }
        if (rMsg.getKey() != null) {
            // 'key' exists
            return;
        }
        Map<Object, Object> keys = rMsg.getKeys();
        if (keys == null) {
            keys = new HashMap<>();
        } else if (keys.get("digest") != null) {
            // key digest already exists
            return;
        }
        // get key with direction
        SymmetricKey key;
        EntityDelegate facebook = getEntityDelegate();
        Object sender = rMsg.envelope.sender;
        Object group = rMsg.envelope.getGroup();
        if (group == null) {
            Object receiver = rMsg.envelope.receiver;
            key = getCipherKeyDelegate().getCipherKey(facebook.getID(sender), facebook.getID(receiver));
        } else {
            key = getCipherKeyDelegate().getCipherKey(facebook.getID(sender), facebook.getID(group));
        }
        // get key data
        byte[] data = key.getData();
        if (data == null || data.length < 6) {
            return;
        }
        // get digest
        byte[] part = new byte[6];
        System.arraycopy(data, data.length-6, part, 0, 6);
        byte[] digest = SHA256.digest(part);
        String base64 = Base64.encode(digest);
        int pos = base64.length() - 8;
        keys.put("digest", base64.substring(pos));
        rMsg.put("keys", keys);
    }

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        // public
        return super.deserializeMessage(data);
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        SecureMessage sMsg = super.encryptMessage(iMsg);

        EntityDelegate facebook = getEntityDelegate();
        Envelope env = iMsg.envelope;
        ID receiver = facebook.getID(env.receiver);
        if (receiver.isGroup()) {
            CipherKeyDelegate keyCache = getCipherKeyDelegate();
            // reuse group message keys
            ID sender = facebook.getID(env.sender);
            SymmetricKey key = keyCache.getCipherKey(sender, receiver);
            key.put("reused", true);
        }
        // TODO: reuse personal message key?

        return sMsg;
    }

    @Override
    public byte[] serializeKey(Map<String, Object> password, InstantMessage iMsg) {
        if (password.get("reused") != null) {
            ID receiver = getEntityDelegate().getID(iMsg.envelope.receiver);
            if (receiver.isGroup()) {
                // reuse key for grouped message
                return null;
            }
        }
        return super.serializeKey(password, iMsg);
    }

    @Override
    public InstantMessage process(InstantMessage iMsg) {
        Content content = iMsg.content;
        ID sender = getFacebook().getID(iMsg.envelope.sender);

        if (checkGroup(content, sender)) {
            // save this message in a queue to wait group meta response
            suspendMessage(iMsg);
            return null;
        }

        iMsg = super.process(iMsg);
        if (iMsg == null) {
            // respond nothing
            return null;
        }
        if (iMsg.content instanceof HandshakeCommand) {
            // urgent command
            return iMsg;
        }
        /*
        if (iMsg.content instanceof ReceiptCommand) {
            ID receiver = getFacebook().getID(iMsg.envelope.receiver);
            if (NetworkType.Station.equals(receiver.getType())) {
                // no need to respond receipt to station
                return null;
            }
        }
         */
        // normal response
        sendMessage(iMsg, null, false);
        // DON'T respond to station directly
        return null;
    }

    //-------- Send

    public boolean sendCommand(Command cmd) {
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

    static {
        // register CPUs
        CommandProcessor.register(Command.RECEIPT, ReceiptCommandProcessor.class);

        CommandProcessor.register(MuteCommand.MUTE, MuteCommandProcessor.class);
        CommandProcessor.register(BlockCommand.BLOCK, BlockCommandProcessor.class);

        // default content processor
        ContentProcessor.register(ContentType.UNKNOWN, AnyContentProcessor.class);
    }
}
