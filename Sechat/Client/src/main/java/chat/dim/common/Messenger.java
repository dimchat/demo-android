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

import java.nio.charset.Charset;
import java.util.List;

import chat.dim.core.Callback;
import chat.dim.core.CompletionHandler;
import chat.dim.core.Transceiver;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.dkd.SecureMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.Group;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ForwardContent;

public class Messenger extends Transceiver {
    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();

        setSocialNetworkDataSource(Facebook.getInstance());
        setCipherKeyDataSource(KeyStore.getInstance());
    }

    //-------- Transform

    public SecureMessage verifyMessage(ReliableMessage rMsg) {
        // [Meta Protocol] check meta in first contact message
        ID sender = getID(rMsg.envelope.sender);
        Meta meta = getMeta(sender);
        if (meta == null) {
            // first contact, try meta in message package
            try {
                meta = Meta.getInstance(rMsg.getMeta());
                if (meta == null) {
                    // TODO: query meta for sender from DIM network
                    //       (do it by application)
                    return null;
                } else if (meta.matches(sender)) {
                    Facebook facebook = Facebook.getInstance();
                    if (!facebook.saveMeta(meta, sender)) {
                        throw new RuntimeException("save meta error: " + sender + ", " + meta);
                    }
                } else {
                    throw new IllegalArgumentException("meta not match: " + sender + ", " + meta);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }
        return super.verifyMessage(rMsg);
    }

    public InstantMessage decryptMessage(SecureMessage sMsg) {
        InstantMessage iMsg = super.decryptMessage(sMsg);

        // check: top-secret message
        if (iMsg.content.type == ContentType.FORWARD.value) {
            // do it again to drop the wrapper,
            // the secret inside the content is the real message
            ForwardContent content = (ForwardContent) iMsg.content;
            ReliableMessage rMsg = content.forwardMessage;

            InstantMessage secret = verifyAndDecryptMessage(rMsg);
            if (secret != null) {
                return secret;
            }
            // FIXME: not for you?
        }

        return iMsg;
    }

    //-------- Convenient

    /**
     *  Pack instant message to reliable message for delivering
     *
     * @param iMsg - instant message
     * @return ReliableMessage Object
     */
    public ReliableMessage encryptAndSignMessage(InstantMessage iMsg) {

        // 1. encrypt 'content' to 'data' for receiver
        SecureMessage sMsg = encryptMessage(iMsg);

        // 1.1. check group
        Object group = iMsg.getGroup();
        if (group != null) {
            sMsg.setGroup(group);
        }

        // 2. sign 'data' by sender
        return signMessage(sMsg);
    }

    /**
     *  Extract instant message from a reliable message received
     *
     * @param rMsg - reliable message
     * @return InstantMessage object
     */
    public InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg) {

        // 1. verify 'data' with 'signature'
        SecureMessage sMsg = verifyMessage(rMsg);

        // 2. check group message
        ID receiver = getID(sMsg.envelope.receiver);
        if (receiver.getType().isGroup()) {
            // TODO: split it
        }

        // 3. decrypt 'data' to 'content'
        return decryptMessage(sMsg);
    }

    //-------- Send

    /**
     *  Send message (secured + certified) to target station
     *
     * @param iMsg - instant message
     * @param callback - callback function
     * @param split - if it's a group message, split it before sending out
     * @return NO on data/delegate error
     */
    public boolean sendMessage(InstantMessage iMsg, Callback callback, boolean split) {
        // transforming
        ReliableMessage rMsg = encryptAndSignMessage(iMsg);
        if (rMsg == null) {
            // TODO: set iMsg.state = error
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }

        // trying to send out
        boolean OK = true;
        ID receiver = getID(iMsg.envelope.receiver);
        if (split && receiver.getType().isGroup()) {
            Group group = getGroup(receiver);
            List<ID> members = group == null ? null : group.getMembers();
            List<SecureMessage> messages = members == null ? null : rMsg.split(members);
            if (messages == null || messages.size() == 0) {
                // failed to split msg, send it to group
                OK = sendMessage(rMsg, callback);
            } else {
                // sending group message one by one
                for (SecureMessage msg: messages) {
                    if (!sendMessage((ReliableMessage) msg, callback)) {
                        OK = false;
                    }
                }
            }
        } else {
            OK = sendMessage(rMsg, callback);
        }

        // TODO: if OK, set iMsg.state = sending; else, set iMsg.state = waiting;
        return OK;
    }

    private boolean sendMessage(final ReliableMessage rMsg, final Callback callback) {
        CompletionHandler handler = new CompletionHandler() {
            @Override
            public void onSuccess() {
                callback.onFinished(rMsg, null);
            }

            @Override
            public void onFailed(Error error) {
                callback.onFinished(rMsg, error);
            }
        };
        String json = JSON.encode(rMsg);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        return sendPackage(data, handler);
    }
}
