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
package chat.dim.common;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;
import chat.dim.digest.SHA256;
import chat.dim.format.Base64;
import chat.dim.mkm.User;
import chat.dim.mtp.MsgUtils;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.Visa;

public class MessagePacker extends chat.dim.MessagePacker {
    public static final int MTP_JSON = 0x01;
    public static final int MTP_DMTP = 0x02;

    // Message Transfer Protocol
    public int mtpFormat = MTP_JSON;

    public MessagePacker(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    protected Messenger getMessenger() {
        return (Messenger) super.getMessenger();
    }

    private void attachKeyDigest(ReliableMessage rMsg) {
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getMessenger());
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
            key = getMessenger().getCipherKey(sender, receiver, false);
        } else {
            key = getMessenger().getCipherKey(sender, group, false);
        }
        if (key == null) {
            // broadcast message has no key
            return;
        }
        // get key data
        byte[] data = key.getData();
        if (data == null || data.length < 6) {
            if (key.getAlgorithm().equalsIgnoreCase("PLAIN")) {
                // broadcast message has no key
                return;
            }
            throw new NullPointerException("key data error: " + key.toMap());
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
    public byte[] serializeMessage(ReliableMessage rMsg) {
        attachKeyDigest(rMsg);
        fixMeta(rMsg);
        if (mtpFormat == MTP_JSON) {
            // JsON
            return super.serializeMessage(rMsg);
        } else {
            // D-MTP
            return MsgUtils.serializeMessage(rMsg);
        }
    }

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        if (data == null || data.length < 2) {
            return null;
        }
        if (data[0] == '{') {
            // JsON
            ReliableMessage rMsg = super.deserializeMessage(data);
            fixMeta(rMsg);
            fixVisa(rMsg);
            return rMsg;
        } else { // D-MTP
            return MsgUtils.deserializeMessage(data);
        }
    }

    @Override
    public ReliableMessage signMessage(SecureMessage sMsg) {
        if (sMsg instanceof ReliableMessage) {
            // already signed
            return (ReliableMessage) sMsg;
        } else {
            return super.signMessage(sMsg);
        }
    }

    @Override
    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        final ID sender = rMsg.getSender();
        // [Meta Protocol]
        Meta meta = rMsg.getMeta();
        if (meta == null) {
            meta = getFacebook().getMeta(sender);
        } else if (!Meta.matches(sender, meta)) {
            meta = null;
        }
        if (meta == null) {
            // NOTICE: the application will query meta automatically,
            //         save this message in a queue waiting sender's meta response
            getMessenger().suspendMessage(rMsg);
            return null;
        }
        // make sure meta exists before verifying message
        return super.verifyMessage(rMsg);
    }

    private boolean isWaiting(ID identifier) {
        if (identifier.isGroup()) {
            // checking group meta
            return getFacebook().getMeta(identifier) == null;
        } else {
            // checking visa key
            return getFacebook().getPublicKeyForEncryption(identifier) == null;
        }
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        final ID receiver = iMsg.getReceiver();
        final ID group = iMsg.getGroup();
        if (!(receiver.isBroadcast() || (group != null  && group.isBroadcast()))) {
            // this message is not a broadcast message
            if (isWaiting(receiver) || (group != null && isWaiting(group))) {
                // NOTICE: the application will query visa automatically,
                //         save this message in a queue waiting sender's visa response
                getMessenger().suspendMessage(iMsg);
                return null;
            }
        }

        // make sure visa.key exists before encrypting message
        SecureMessage sMsg = super.encryptMessage(iMsg);

        if (receiver.isGroup()) {
            // reuse group message keys
            ID sender = iMsg.getSender();
            SymmetricKey key = getMessenger().getCipherKey(sender, receiver, false);
            assert key != null : "failed to get msg key for: " + sender + " -> " + receiver;
            key.put("reused", true);
        }
        // TODO: reuse personal message key?

        return sMsg;
    }

    @Override
    public InstantMessage decryptMessage(SecureMessage sMsg) {
        try {
            InstantMessage iMsg = super.decryptMessage(sMsg);
            fixProfile(iMsg.getContent());
            return iMsg;
        } catch (NullPointerException e) {
            // check exception thrown by DKD: chat.dim.dkd.EncryptedMessage.decrypt()
            if (e.getMessage().contains("failed to decrypt key in msg: ")) {
                // visa.key not updated?
                User user = getFacebook().getCurrentUser();
                Visa visa = user.getVisa();
                if (visa == null || !visa.isValid()) {
                    // FIXME: user visa not found?
                    throw new NullPointerException("user visa error: " + user.getIdentifier());
                }
                Command cmd = DocumentCommand.response(user.getIdentifier(), visa);
                getMessenger().sendContent(user.getIdentifier(), sMsg.getSender(), cmd, 0);
            } else {
                // FIXME: message error?
                throw e;
            }
            return null;
        } catch (Exception e) {
            // FIXME: cipher text error?
            e.printStackTrace();
            return null;
        }
    }

    private void fixProfile(Content content) {
        if (content instanceof DocumentCommand) {
            // compatible for document command
            Object doc = content.get("document");
            if (doc != null) {
                // (v2.0)
                //    "ID"       : "{ID}",
                //    "document" : {
                //        "ID"        : "{ID}",
                //        "data"      : "{JsON}",
                //        "signature" : "{BASE64}"
                //    }
                return;
            }
            Object profile = content.get("profile");
            if (profile != null) {
                content.remove("profile");
                // 1.* => 2.0
                if (profile instanceof String) {
                    // compatible with v1.0
                    //    "ID"        : "{ID}",
                    //    "profile"   : "{JsON}",
                    //    "signature" : "{BASE64}"
                    Map<String, Object> dict = new HashMap<>();
                    dict.put("ID", content.get("ID"));
                    dict.put("data", profile);
                    dict.put("signature", content.get("signature"));
                    content.put("document", dict);
                } else {
                    // compatible with v1.1
                    //    "ID"       : "{ID}",
                    //    "profile"  : {
                    //        "ID"        : "{ID}",
                    //        "data"      : "{JsON}",
                    //        "signature" : "{BASE64}"
                    //    }
                    content.put("document", profile);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void fixMeta(ReliableMessage rMsg) {
        Object attachment = rMsg.get("meta");
        if (attachment != null) {
            Map<String, Object> meta = (Map<String, Object>) attachment;
            Object version = meta.get("version");
            if (version == null) {
                version = meta.get("type");
            }
            meta.put("type", version);
            meta.put("version", version);
        }
    }
    private void fixVisa(ReliableMessage rMsg) {
        Object profile = rMsg.get("profile");
        if (profile != null) {
            rMsg.remove("profile");
            // 1.* => 2.0
            Object visa = rMsg.get("visa");
            if (visa == null) {
                rMsg.put("visa", profile);
            }
        }
    }
}
