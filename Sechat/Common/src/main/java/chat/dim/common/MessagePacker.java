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

import chat.dim.CipherKeyDelegate;
import chat.dim.core.Packer;
import chat.dim.crypto.SymmetricKey;
import chat.dim.digest.SHA256;
import chat.dim.format.Base64;
import chat.dim.mtp.Utils;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;

public class MessagePacker extends Packer {
    public static final int MTP_JSON = 0x01;
    public static final int MTP_DMTP = 0x02;

    // Message Transfer Protocol
    public int mtpFormat = MTP_DMTP;

    public MessagePacker(Facebook barrack, Messenger transceiver, KeyStore keyCache) {
        super(barrack, transceiver, keyCache);
    }

    private SymmetricKey getCipherKey(ID sender, ID receiver, boolean generate) {
        CipherKeyDelegate delegate = getCipherKeyDelegate();
        return delegate.getCipherKey(sender, receiver, generate);
    }

    private void attachKeyDigest(ReliableMessage rMsg) {
        if (rMsg.getDelegate() == null) {
            rMsg.setDelegate(getMessageDelegate());
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
            key = getCipherKey(sender, receiver, false);
        } else {
            key = getCipherKey(sender, group, false);
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
            rMsg.setDelegate(getMessageDelegate());
        }
        return rMsg;
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        SecureMessage sMsg = super.encryptMessage(iMsg);

        ID receiver = iMsg.getReceiver();
        if (ID.isGroup(receiver)) {
            // reuse group message keys
            ID sender = iMsg.getSender();
            SymmetricKey key = getCipherKey(sender, receiver, false);
            assert key != null : "failed to get msg key for: " + sender + " -> " + receiver;
            key.put("reused", true);
        }
        // TODO: reuse personal message key?

        return sMsg;
    }
}
