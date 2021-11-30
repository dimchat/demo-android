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
import java.util.Date;

import chat.dim.common.Messenger;
import chat.dim.net.Hub;
import chat.dim.port.Gate;
import chat.dim.port.Ship;
import chat.dim.protocol.ReliableMessage;
import chat.dim.skywalker.Runner;
import chat.dim.stargate.CommonGate;

public abstract class GateKeeper<G extends CommonGate<H>, H extends Hub> extends Runner {

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

    protected void storeMessage(ReliableMessage msg) {
        // TODO: store the stranded message?
    }

    private void flush() {
        // store all messages
        ReliableMessage msg;
        MessageWrapper wrapper = queue.shift();
        while (wrapper != null) {
            msg = wrapper.getMessage();
            if (msg != null) {
                storeMessage(msg);
            }
            wrapper = queue.shift();
        }
    }

    private void clean() {
        long now = (new Date()).getTime();
        // store expired message
        ReliableMessage msg;
        MessageWrapper wrapper = queue.eject(now);
        while (wrapper != null) {
            msg = wrapper.getMessage();
            if (msg != null) {
                storeMessage(msg);
            }
            wrapper = queue.eject(now);
        }
    }

    @Override
    public void finish() {
        super.finish();
        flush();
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
        clean();
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
     * @param payload
     * @param priority
     * @param delegate
     * @return
     */
    public boolean send(byte[] payload, int priority, Ship.Delegate delegate) {
        return gate.send(null, remote, payload, priority, delegate);
    }

    /**
     *  Push message into a waiting queue
     *
     * @param msg
     * @return
     */
    public boolean push(ReliableMessage msg) {
        return queue.append(msg);
    }
}
