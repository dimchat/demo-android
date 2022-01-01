/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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

import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import chat.dim.Transmitter;
import chat.dim.User;
import chat.dim.common.Messenger;
import chat.dim.net.Hub;
import chat.dim.port.Gate;
import chat.dim.port.Ship;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.skywalker.Runner;
import chat.dim.stargate.CommonGate;
import chat.dim.threading.BackgroundThreads;

public abstract class GateKeeper<G extends CommonGate<H>, H extends Hub>
        extends Runner implements Transmitter {

    private final SocketAddress remote;

    public final G gate;
    private final WeakReference<Messenger> messengerRef;

    private final MessageQueue queue;

    private boolean active;

    public GateKeeper(String host, int port, Gate.Delegate delegate, Messenger transceiver) {
        super();
        remote = new InetSocketAddress(host, port);
        gate = createGate(host, port, delegate);
        messengerRef = new WeakReference<>(transceiver);
        queue = new MessageQueue();
        // session status
        active = false;
    }

    protected abstract G createGate(String host, int port, Gate.Delegate delegate);

    public boolean isActive() {
        return active;
    }
    public void setActive(boolean value) {
        active = value;
    }

    public Messenger getMessenger() {
        return messengerRef.get();
    }

    @Override
    public boolean process() {
        boolean incoming = gate.getHub().process();
        boolean outgoing = gate.process();
        if (incoming || outgoing) {
            // processed income/outgo packages
            return true;
        } else if (!isActive()) {
            // inactive, wait a while to check again
            queue.purge();
            return false;
        }
        // get next message
        ReliableMessage msg;
        MessageWrapper wrapper = queue.next();
        if (wrapper == null) {
            // no more new message
            queue.purge();
            return false;
        }
        // if msg in this wrapper is None (means sent successfully),
        // it must have been cleaned already, so it should not be empty here.
        msg = wrapper.getMessage();
        if (msg == null) {
            // msg sent?
            return true;
        }
        // try to push
        byte[] data = getMessenger().serializeMessage(msg);
        if (send(data, wrapper.getPriority(), wrapper)) {
            wrapper.onSuccess();
        } else {
            Error error = new Error("gate error, failed to send data");
            wrapper.onFailed(error);
        }
        return true;
    }

    /**
     *  Send data via the gate
     *
     * @param payload  - encode message
     * @param priority - smaller is faster
     * @param delegate - callback
     * @return false on duplicated
     */
    public boolean send(byte[] payload, int priority, Ship.Delegate delegate) {
        return gate.send(null, remote, payload, priority, delegate);
    }

    /**
     *  Push message into a waiting queue
     *
     * @param rMsg     - network message
     * @param priority - smaller is faster
     * @return True
     */
    @Override
    public boolean sendMessage(ReliableMessage rMsg, int priority) {
        return queue.append(rMsg, priority);
    }

    /**
     *  Push message into a waiting queue after encrypted and signed
     *
     * @param iMsg     - plain message
     * @param priority - smaller is faster
     * @return True
     */
    @Override
    public boolean sendMessage(InstantMessage iMsg, int priority) {
        BackgroundThreads.wait(() -> {
            Messenger messenger = getMessenger();
            // Send message (secured + certified) to target station
            SecureMessage sMsg = messenger.encryptMessage(iMsg);
            if (sMsg == null) {
                // public key not found?
                return ;
                //throw new NullPointerException("failed to encrypt message: " + iMsg);
            }
            ReliableMessage rMsg = messenger.signMessage(sMsg);
            if (rMsg == null) {
                // TODO: set iMsg.state = error
                throw new NullPointerException("failed to sign message: " + sMsg);
            }

            sendMessage(rMsg, priority);
            // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting

            // save signature for receipt
            iMsg.put("signature", rMsg.get("signature"));

            messenger.saveMessage(iMsg);
        });
        return true;
    }

    /**
     *  Send message content with priority
     *
     * @param sender   - from who
     * @param receiver - to who
     * @param content  - message content
     * @param priority - smaller is faster
     * @return True
     */
    @Override
    public boolean sendContent(ID sender, ID receiver, Content content, int priority) {
        // Application Layer should make sure user is already login before it send message to server.
        // Application layer should put message into queue so that it will send automatically after user login
        if (sender == null) {
            User user = getMessenger().getFacebook().getCurrentUser();
            if (user == null) {
                throw new NullPointerException("current user not set");
            }
            sender = user.identifier;
        }
        Envelope env = Envelope.create(sender, receiver, null);
        InstantMessage iMsg = InstantMessage.create(env, content);
        return sendMessage(iMsg, priority);
    }
}