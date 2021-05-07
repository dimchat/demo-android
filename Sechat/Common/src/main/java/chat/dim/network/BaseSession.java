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
import java.net.Socket;

import chat.dim.common.Messenger;
import chat.dim.protocol.ReliableMessage;
import chat.dim.stargate.Gate;
import chat.dim.stargate.Ship;
import chat.dim.stargate.StarGate;
import chat.dim.utils.Log;

public class BaseSession extends Thread implements Gate.Delegate {

    public static int EXPIRES = 600 * 1000;  // 10 minutes

    public final StarGate gate;
    private final WeakReference<Messenger> messengerRef;

    private final MessageQueue queue;

    private boolean active;
    private boolean running;

    private BaseSession(StarGate starGate, Messenger transceiver) {
        super();
        gate = starGate;
        gate.setDelegate(this);
        messengerRef = new WeakReference<>(transceiver);
        queue = new MessageQueue();
        // session status
        active = false;
        running = false;
    }
    public BaseSession(String host, int port, Messenger transceiver) {
        this(StarTrek.createGate(host, port), transceiver);
    }
    public BaseSession(Socket socket, Messenger transceiver) {
        this(StarTrek.createGate(socket), transceiver);
    }

    private void flush() {
        // store all messages
        ReliableMessage msg;
        MessageWrapper wrapper = queue.pop();
        while (wrapper != null) {
            msg = wrapper.getMessage();
            if (msg != null) {
                storeMessage(msg);
            }
            wrapper = queue.pop();
        }
    }

    private void clean() {
        // store expired message
        ReliableMessage msg;
        MessageWrapper wrapper = queue.eject();
        while (wrapper != null) {
            msg = wrapper.getMessage();
            if (msg != null) {
                storeMessage(msg);
            }
            wrapper = queue.pop();
        }
    }

    protected void storeMessage(ReliableMessage msg) {
        // TODO: store the stranded message?
    }

    public Messenger getMessenger() {
        return messengerRef.get();
    }

    public boolean isActive() {
        return active && gate.isOpened();
    }
    public void setActive(boolean value) {
        active = value;
    }

    @Override
    public void run() {
        setup();
        try {
            handle();
        } finally {
            finish();
        }
    }

    public void close() {
        running = false;
    }

    public void setup() {
        running = true;
        gate.setup();
    }

    public void finish() {
        running = false;
        gate.finish();
        flush();
    }

    public boolean isRunning() {
        return running && gate.isOpened();
    }

    public void handle() {
        while (isRunning()) {
            if (!process()) {
                idle();
            }
        }
    }

    protected void idle() {
        try {
            Thread.sleep(128);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean process() {
        if (gate.process()) {
            // processed income/outgo packages
            return true;
        }
        clean();
        if (!isActive()) {
            // inactive
            return false;
        }
        // get next message
        ReliableMessage msg;
        MessageWrapper wrapper = queue.next();
        if (wrapper == null) {
            // no more new message
            msg = null;
        } else {
            // if msg in this wrapper is None (means sent successfully),
            // it must have been cleaned already, so it should not be empty here.
            msg = wrapper.getMessage();
        }
        if (msg == null) {
            // no more new message
            return false;
        }
        // try to push
        if (!getMessenger().sendMessage(msg, wrapper, 0)) {
            wrapper.fail();
        }
        return true;
    }

    public boolean push(ReliableMessage rMsg) {
        if (isActive()) {
            return queue.append(rMsg);
        } else {
            return false;
        }
    }

    //
    //  Gate Delegate
    //

    @Override
    public void onStatusChanged(Gate gate, Gate.Status oldStatus, Gate.Status newStatus) {
        if (newStatus.equals(Gate.Status.Connected)) {
            getMessenger().onConnected();
        }
    }

    @Override
    public byte[] onReceived(Gate gate, Ship ship) {
        byte[] payload = ship.getPayload();
        Log.info("received " + payload.length + " bytes");
        try {
            return getMessenger().process(payload);
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        }
    }
}
