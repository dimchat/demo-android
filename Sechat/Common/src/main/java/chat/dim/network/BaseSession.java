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

import com.alibaba.fastjson.JSONException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.SocketAddress;

import chat.dim.common.Messenger;
import chat.dim.mtp.Package;
import chat.dim.mtp.StreamArrival;
import chat.dim.net.Connection;
import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Gate;
import chat.dim.port.Ship;
import chat.dim.protocol.ReliableMessage;
import chat.dim.startrek.DepartureShip;
import chat.dim.utils.Log;

public class BaseSession extends Thread implements Gate.Delegate {

    public static int EXPIRES = 600 * 1000;  // 10 minutes

    public final StarTrek gate;
    private final WeakReference<Messenger> messengerRef;

    private final MessageQueue queue;

    private boolean active;

    public BaseSession(String host, int port, Messenger transceiver) throws IOException {
        super();
        gate = StarTrek.create(host, port, this);
        messengerRef = new WeakReference<>(transceiver);
        queue = new MessageQueue();
        // session status
        active = false;
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
        // store expired message
        ReliableMessage msg;
        MessageWrapper wrapper = queue.eject();
        while (wrapper != null) {
            msg = wrapper.getMessage();
            if (msg != null) {
                storeMessage(msg);
            }
            wrapper = queue.eject();
        }
    }

    protected void storeMessage(ReliableMessage msg) {
        // TODO: store the stranded message?
    }

    public Messenger getMessenger() {
        return messengerRef.get();
    }

    public boolean isActive() {
        return active && gate.isRunning();
    }
    public void setActive(boolean value) {
        active = value;
    }

    @Override
    public synchronized void start() {
        super.start();
    }

    @Override
    public void run() {
        setup();
        try {
            handle();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            finish();
        }
    }

    public void close() {
        gate.stop();
    }

    public void setup() {
        gate.start();
    }

    public void finish() {
        gate.stop();
        flush();
    }

    public boolean isRunning() {
        return gate.isRunning();
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
    public void onStatusChanged(Gate.Status oldStatus, Gate.Status newStatus, SocketAddress remote, SocketAddress local, Gate gate) {
        if (newStatus.equals(Gate.Status.READY)) {
            getMessenger().onConnected();
        }
    }

    @Override
    public void onReceived(Arrival arrival, SocketAddress source, SocketAddress destination, Connection connection) {
        assert arrival instanceof StreamArrival : "arrival ship error: " + arrival;
        StreamArrival ship = (StreamArrival) arrival;
        Package pack = ship.getPackage();
        byte[] payload = pack.body.getBytes();
        Log.info("received " + payload.length + " bytes");
        try {
            getMessenger().process(payload);
        } catch (JSONException e) {
            Log.info("JSON error: " + (new String(payload)));
        } catch (NullPointerException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSent(Departure departure, SocketAddress source, SocketAddress destination, Connection connection) {
        if (departure instanceof DepartureShip) {
            Ship.Delegate delegate = ((DepartureShip) departure).getDelegate();
            if (delegate != null && delegate != this) {
                delegate.onSent(departure, source, destination, connection);
            }
        }
    }

    @Override
    public void onError(Throwable error, Departure departure, SocketAddress source, SocketAddress destination, Connection connection) {
        Log.error("connection error (" + source + ", " + destination + "): " + error.getLocalizedMessage());
        if (departure instanceof DepartureShip) {
            Ship.Delegate delegate = ((DepartureShip) departure).getDelegate();
            if (delegate != null && delegate != this) {
                delegate.onError(error, departure, source, destination, connection);
            }
        }
    }
}
