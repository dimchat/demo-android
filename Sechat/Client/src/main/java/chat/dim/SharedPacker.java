/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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

import java.io.IOException;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.mkm.User;
import chat.dim.model.MessageDataSource;
import chat.dim.mtp.MsgUtils;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.Visa;
import chat.dim.utils.Log;

public class SharedPacker extends ClientMessagePacker {

    public static final int MTP_JSON = 0x01;
    public static final int MTP_DMTP = 0x02;

    // Message Transfer Protocol
    public int mtpFormat = MTP_JSON;

    public SharedPacker(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public byte[] serializeMessage(ReliableMessage rMsg) {
        Compatible.fixMetaAttachment(rMsg);
        if (mtpFormat == MTP_JSON) {
            // JsON
            return super.serializeMessage(rMsg);
        } else {
            // D-MTP
            // TODO: attachKeyDigest(rMsg, getMessenger());
            return MsgUtils.serializeMessage(rMsg);
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
        } else {
            // D-MTP
            rMsg = MsgUtils.deserializeMessage(data);
            if (rMsg != null) {
                // FIXME: just change it when first package received
                mtpFormat = MTP_DMTP;
            }
        }
        if (rMsg != null) {
            Compatible.fixMetaAttachment(rMsg);
        }
        return rMsg;
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        // make sure visa.key exists before encrypting message
        Content content = iMsg.getContent();
        if (content instanceof FileContent) {
            FileContent fileContent = (FileContent) content;
            if (fileContent.getData() != null/* && fileContent.getURL() == null*/) {
                SymmetricKey key = getMessenger().getEncryptKey(iMsg);
                assert key != null : "failed to get msg key: "
                        + iMsg.getSender() + " => " + iMsg.getReceiver() + ", " + iMsg.get("group");
                // call emitter to encrypt & upload file data before send out
                GlobalVariable shared = GlobalVariable.getInstance();
                try {
                    shared.emitter.sendFileContentMessage(iMsg, key);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        }
        // check receiver & encrypt
        return super.encryptMessage(iMsg);
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        InstantMessage iMsg = super.decryptMessage(sMsg);
        if (iMsg == null) {
            // failed to decrypt message, visa.key changed?
            // 1. push new visa document to this message sender
            pushVisa(sMsg.getSender());
            // 2. build 'failed' message
            iMsg = getFailedMessage(sMsg);
        } else {
            Content content = iMsg.getContent();
            if (content instanceof FileContent) {
                FileContent fileContent = (FileContent) content;
                if (fileContent.getPassword() == null && ((FileContent) content).getURL() != null) {
                    // now received file content with remote data,
                    // which must be encrypted before upload to CDN;
                    // so keep the password here for decrypting after downloaded.
                    SymmetricKey key = getMessenger().getDecryptKey(sMsg);
                    assert key != null : "failed to get msg key: "
                            + sMsg.getSender() + " => " + sMsg.getReceiver() + ", " + sMsg.get("group");
                    // keep password to decrypt data after downloaded
                    fileContent.setPassword(key);
                }
            }
        }
        return iMsg;
    }

    protected void pushVisa(ID contact) {
        GlobalVariable shared = GlobalVariable.getInstance();
        if (!shared.archivist.isDocumentResponseExpired(contact, false)) {
            // response not expired yet
            Log.debug("visa push not expired yet: " + contact);
            return;
        }
        Log.info("push visa to: " + contact);
        User user = getFacebook().getCurrentUser();
        Visa visa = user.getVisa();
        if (visa == null || !visa.isValid()) {
            // FIXME: user visa not found?
            assert false : "user visa error: " + user;
            return;
        }
        ID me = user.getIdentifier();
        Content command = DocumentCommand.response(me, visa);
        CommonMessenger messenger = (CommonMessenger) getMessenger();
        messenger.sendContent(me, contact, command, 1);
    }

    protected InstantMessage getFailedMessage(SecureMessage sMsg) {
        ID sender = sMsg.getSender();
        ID group = sMsg.getGroup();
        int type = sMsg.getType();
        if (ContentType.COMMAND.equals(type) || ContentType.HISTORY.equals(type)) {
            Log.warning("ignore message unable to decrypt (type=" + type + ") from " + sender);
            return null;
        }
        // create text content
        Content content = TextContent.create("Failed to decrypt message.");
        content.put("template", "Failed to decrypt message (type=${type}) from '${sender}'");
        content.put("replacements", newMap(
                "type", type,
                "sender", sender.toString(),
                "group", group == null ? null : group.toString()
        ));
        if (group != null) {
            content.setGroup(group);
        }
        // pack instant message
        Map<String, Object> info = sMsg.copyMap(false);
        info.remove("data");
        info.put("content", content.toMap());
        return InstantMessage.parse(info);
    }

    @Override
    protected void suspendMessage(ReliableMessage rMsg, Map<String, ?> info) {
        rMsg.put("error", info);
        MessageDataSource mds = MessageDataSource.getInstance();
        mds.suspendMessage(rMsg);
    }

    @Override
    protected void suspendMessage(InstantMessage iMsg, Map<String, ?> info) {
        iMsg.put("error", info);
        MessageDataSource mds = MessageDataSource.getInstance();
        mds.suspendMessage(iMsg);
    }

}
