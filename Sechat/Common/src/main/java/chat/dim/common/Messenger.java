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

import chat.dim.Callback;
import chat.dim.core.CipherKeyDelegate;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.cpu.BlockCommandProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.MuteCommandProcessor;
import chat.dim.cpu.ReceiptCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.digest.SHA256;
import chat.dim.format.Base64;
import chat.dim.mkm.BroadcastAddress;
import chat.dim.mtp.Utils;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.threading.BackgroundThreads;

public abstract class Messenger extends chat.dim.Messenger {

    public static final int MTP_JSON = 0x01;
    public static final int MTP_DMTP = 0x02;

    // Message Transfer Protocol
    public int mtpFormat = MTP_DMTP;

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
        if (group == null || group.getAddress() instanceof BroadcastAddress) {
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
        if (mtpFormat == MTP_JSON) {
            // JsON
            return super.serializeMessage(rMsg);
        } else {
            // D-MTP
            return Utils.serializeMessage(rMsg);
        }
    }

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        if (data == null || data.length < 2) {
            return null;
        }
        ReliableMessage rMsg;
        if (data[0] == '{') {
            // JsON
            rMsg = super.deserializeMessage(data);
        } else { // D-MTP
            rMsg = Utils.deserializeMessage(data);
        }
        if (rMsg != null) {
            rMsg.setDelegate(this);
        }
        return rMsg;
    }

    private void attachKeyDigest(ReliableMessage rMsg) {
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(this);
        }
        if (rMsg.getEncryptedKey() != null) {
            // 'key' exists
            return;
        }
        Map<String, Object> keys = rMsg.getEncryptedKeys();
        if (keys == null) {
            keys = new HashMap<>();
        } else if (keys.get("digest") != null) {
            // key digest already exists
            return;
        }
        // get key with direction
        SymmetricKey key;
        ID sender = rMsg.getSender();
        ID group = rMsg.getGroup();
        if (group == null) {
            ID receiver = rMsg.getReceiver();
            key = getCipherKeyDelegate().getCipherKey(sender, receiver);
        } else {
            key = getCipherKeyDelegate().getCipherKey(sender, group);
        }
        // get key data
        byte[] data = key.getData();
        if (data == null || data.length < 6) {
            // broadcast message has no key
            return;
        }
        // get digest
        byte[] part = new byte[6];
        System.arraycopy(data, data.length-6, part, 0, 6);
        byte[] digest = SHA256.digest(part);
        String base64 = Base64.encode(digest);
        base64 = base64.trim();
        int pos = base64.length() - 8;
        keys.put("digest", base64.substring(pos));
        rMsg.put("keys", keys);
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        SecureMessage sMsg = super.encryptMessage(iMsg);

        ID receiver = iMsg.getReceiver();
        if (NetworkType.isGroup(receiver.getType())) {
            CipherKeyDelegate keyCache = getCipherKeyDelegate();
            // reuse group message keys
            ID sender = iMsg.getSender();
            SymmetricKey key = keyCache.getCipherKey(sender, receiver);
            key.put("reused", true);
        }
        // TODO: reuse personal message key?

        return sMsg;
    }

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        Object reused = password.get("reused");
        if (reused != null) {
            ID receiver = iMsg.getReceiver();
            if (NetworkType.isGroup(receiver.getType())) {
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
                SecureMessage sMsg = encryptMessage(iMsg);
                if (sMsg == null) {
                    // public key not found?
                    return ;
                    //throw new NullPointerException("failed to encrypt message: " + iMsg);
                }
                ReliableMessage rMsg = signMessage(sMsg);
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

    static {
        // register CPUs
        CommandProcessor.register(Command.RECEIPT, ReceiptCommandProcessor.class);

        CommandProcessor.register(MuteCommand.MUTE, MuteCommandProcessor.class);
        CommandProcessor.register(BlockCommand.BLOCK, BlockCommandProcessor.class);

        // default content processor
        ContentProcessor.register(ContentType.UNKNOWN, AnyContentProcessor.class);
    }
}
