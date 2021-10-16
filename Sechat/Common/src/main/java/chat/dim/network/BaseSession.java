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
import java.util.ArrayList;
import java.util.List;

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

    private final StarTrek gate;
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

    public StarTrek getGate() {
        return gate;
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
        active = false;
        //gate.stop();
    }

    public void setup() {
        gate.start();
    }

    public void finish() {
        active = false;
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
        if (newStatus == null || newStatus.equals(Gate.Status.ERROR)) {
            setActive(false);
            //close();
        } else if (newStatus.equals(Gate.Status.READY)) {
            getMessenger().onConnected();
        }
    }

    private static List<byte[]> splitLines(byte[] data) {
        List<byte[]> lines = new ArrayList<>();
        byte[] tmp;
        int pos1 = 0, pos2;
        while (pos1 < data.length) {
            pos2 = pos1;
            while (pos2 < data.length) {
                if (data[pos2] == '\n') {
                    break;
                } else {
                    ++pos2;
                }
            }
            if (pos2 > pos1) {
                tmp = new byte[pos2 - pos1];
                System.arraycopy(data, pos1, tmp, 0, pos2 - pos1);
                lines.add(tmp);
            }
            pos1 = pos2 + 1;  // skip '\n'
        }
        return lines;
    }
    private static byte[] join(List<byte[]> packages) {
        // get buffer size
        int size = 0;
        for (byte[] item : packages) {
            size += item.length + 1;
        }
        if (size == 0) {
            return null;
        } else {
            size -= 1;  // remove last '\n'
        }
        // combine packages
        byte[] buffer = new byte[size];
        // copy first package
        byte[] item = packages.get(0);
        System.arraycopy(item, 0, buffer, 0, item.length);
        // copy the others
        int offset = item.length;
        for (int i = 0; i < packages.size(); ++i) {
            // set separator
            buffer[offset] = SEPARATOR;
            ++offset;
            // copy package data
            item = packages.get(i);
            System.arraycopy(item, 0, buffer, offset, item.length);
            offset += item.length;
        }
        return buffer;
    }
    private static final byte SEPARATOR = '\n';

    @Override
    public void onReceived(Arrival arrival, SocketAddress source, SocketAddress destination, Connection connection) {
        assert arrival instanceof StreamArrival : "arrival ship error: " + arrival;
        StreamArrival ship = (StreamArrival) arrival;
        Package pack = ship.getPackage();
        byte[] payload = pack.body.getBytes();
        Log.info("received " + payload.length + " bytes");
        // 1. split data when multi packages received in one time
        List<byte[]> packages;
        if (payload.length == 0) {
            packages = new ArrayList<>();
        } else if (payload[0] == '{') {
            packages = splitLines(payload);
        } else {
            packages = new ArrayList<>();
            packages.add(payload);
        }
        // 2. process package data one by one
        Messenger messenger = getMessenger();
        List<byte[]> responses;
        byte[] buffer;
        for (byte[] data : packages) {
            try {
                responses = messenger.process(data);
            } catch (JSONException e) {
                Log.info("JSON error: " + (new String(payload)));
                continue;
            } catch (NullPointerException e) {
                e.printStackTrace();
                continue;
            }
            if (responses == null || responses.size() == 0) {
                continue;
            }
            // combine & respond
            buffer = join(responses);
            if (buffer == null || buffer.length == 0) {
                // should not happen
                continue;
            }
            messenger.sendPackage(buffer, null, Departure.Priority.SLOWER.value);
        }
    }

    @Override
    public void onSent(Departure departure, SocketAddress source, SocketAddress destination, Connection connection) {
        Log.info("message sent: " + departure);
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
