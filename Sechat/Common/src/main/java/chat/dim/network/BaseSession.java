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

import java.util.ArrayList;
import java.util.List;

import chat.dim.Transmitter;
import chat.dim.common.Messenger;
import chat.dim.mtp.Package;
import chat.dim.mtp.StreamArrival;
import chat.dim.net.Hub;
import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Docker;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.skywalker.Runner;
import chat.dim.stargate.BaseGate;
import chat.dim.utils.Log;

public abstract class BaseSession<G extends BaseGate<H>, H extends Hub>
        extends Runner implements Transmitter, Docker.Delegate {

    private final GateKeeper<G, H> keeper;

    public BaseSession(String host, int port, Messenger transceiver) {
        super();
        keeper = createGateKeeper(host, port, transceiver);
    }

    protected abstract GateKeeper<G, H> createGateKeeper(String host, int port, Messenger transceiver);

    public Messenger getMessenger() {
        return keeper.getMessenger();
    }

    public boolean isActive() {
        return keeper.isActive();
    }
    public void setActive(boolean value) {
        keeper.setActive(value);
    }

    public Docker.Status getStatus() {
        return keeper.getStatus();
    }

    public void close() {
        setActive(false);
        //gate.stop();
    }

    @Override
    public void stop() {
        super.stop();
        keeper.stop();
    }

    @Override
    public boolean isRunning() {
        return super.isRunning() && keeper.isRunning();
    }

    @Override
    public void setup() {
        super.setup();
        keeper.setup();
    }

    @Override
    public void finish() {
        keeper.finish();
        super.finish();
    }

    @Override
    public boolean process() {
        return keeper.process();
    }

    public boolean send(byte[] payload, int priority) {
        if (!isActive()) {
            // FIXME: connection lost?
            Log.warning("session inactive: " + keeper.getRemoteAddress());
        }
        Log.info("sending " + payload.length + " byte(s)");
        return keeper.send(payload, priority);
    }

    @Override
    public boolean sendMessage(ReliableMessage rMsg, int priority) {
        if (!isActive()) {
            // FIXME: connection lost?
            Log.warning("session inactive: " + keeper.getRemoteAddress());
        }
        Log.info("sending content to: " + rMsg.getReceiver() + ", priority: " + priority);
        return keeper.sendMessage(rMsg, priority);
    }

    @Override
    public boolean sendMessage(InstantMessage iMsg, int priority) {
        if (!isActive()) {
            // FIXME: connection lost?
            Log.warning("session inactive: " + keeper.getRemoteAddress());
        }
        Log.info("sending content to: " + iMsg.getReceiver() + ", priority: " + priority);
        return keeper.sendMessage(iMsg, priority);
    }

    @Override
    public boolean sendContent(ID sender, ID receiver, Content content, int priority) {
        if (!isActive()) {
            // FIXME: connection lost?
            Log.warning("session inactive: " + keeper.getRemoteAddress());
        }
        Log.info("sending content to: " + receiver + ", priority: " + priority);
        return keeper.sendContent(sender, receiver, content, priority);
    }

    //
    //  Docker Delegate
    //

    @Override
    public void onDockerStatusChanged(Docker.Status previous, Docker.Status current, Docker docker) {
        if (current == null || current.equals(Docker.Status.ERROR)) {
            setActive(false);
            //close();
        } else if (current.equals(Docker.Status.READY)) {
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
    public void onDockerReceived(Arrival arrival, Docker docker) {
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
                responses = messenger.processPackage(data);
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
            keeper.send(buffer, Departure.Priority.SLOWER.value);
        }
    }

    @Override
    public void onDockerSent(Departure departure, Docker docker) {
        Log.info("message sent: " + departure);
//        if (departure instanceof DepartureShip) {
//            Ship.Delegate delegate = ((DepartureShip) departure).getDelegate();
//            if (delegate != null && delegate != this) {
//                delegate.onSent(departure, source, destination, connection);
//            }
//        }
        if (departure instanceof MessageWrapper) {
            ((MessageWrapper) departure).onSent(docker);
        }
    }

    @Override
    public void onDockerFailed(Throwable error, Departure departure, Docker docker) {
        Log.error("connection failed: " + error.getLocalizedMessage());
//        if (departure instanceof DepartureShip) {
//            Ship.Delegate delegate = ((DepartureShip) departure).getDelegate();
//            if (delegate != null && delegate != this) {
//                delegate.onError(error, departure, source, destination, connection);
//            }
//        }
        if (departure instanceof MessageWrapper) {
            ((MessageWrapper) departure).onFailed(error, docker);
        }
    }

    @Override
    public void onDockerError(Throwable error, Departure departure, Docker docker) {
        Log.error("connection error: " + error.getLocalizedMessage());
    }
}
