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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import chat.dim.common.Messenger;
import chat.dim.port.Departure;
import chat.dim.port.Gate;
import chat.dim.port.Ship;
import chat.dim.stargate.TCPClientGate;
import chat.dim.tcp.ClientHub;

public class Session extends BaseSession<TCPClientGate, ClientHub> {

    public Session(String host, int port, Messenger transceiver) {
        super(host, port, transceiver);
    }

    @Override
    protected TCPClientGate createGate(String host, int port, Gate.Delegate delegate) {
        SocketAddress remote = new InetSocketAddress(host, port);
        return new TCPClientGate(delegate, remote, null);
    }

    @Override
    public void setup() {
        setActive(true);
        super.setup();
    }

    @Override
    public void finish() {
        super.finish();
        setActive(false);
    }

    public boolean send(byte[] payload, Departure.Priority priority, Ship.Delegate delegate) {
        return send(payload, priority.value, delegate);
    }
    public boolean send(byte[] payload, int priority, Ship.Delegate delegate) {
        if (isActive()) {
            getGate().sendMessage(payload, priority, delegate);
            return true;
        } else {
            // FIXME: connection lost?
            // java.nio.BufferOverflowException
            return false;
        }
    }

    //
    //  Gate Delegate
    //

    @Override
    public void onStatusChanged(Gate.Status oldStatus, Gate.Status newStatus, SocketAddress remote, SocketAddress local, Gate gate) {
        if (newStatus == null || newStatus.equals(Gate.Status.ERROR)) {
            // connection lost, reconnecting
            ClientHub hub = getGate().getHub();
            hub.connect(remote, local);
        } else if (newStatus.equals(Gate.Status.READY)) {
            getMessenger().onConnected();
            // handshake
            Messenger.Delegate delegate = getMessenger().getDelegate();
            if (delegate instanceof Server) {
                ((Server) delegate).handshake(null);
            }
        }
    }
}
