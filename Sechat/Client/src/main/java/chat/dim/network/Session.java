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

import chat.dim.common.Messenger;
import chat.dim.stargate.MTPDocker;
import chat.dim.startrek.Gate;
import chat.dim.startrek.Ship;

public class Session extends BaseSession {

    private final MTPDocker docker;

    public Session(String host, int port, Messenger transceiver) {
        super(host, port, transceiver);
        docker = new MTPDocker(gate);
    }

    @Override
    public void setup() {
        gate.setDocker(docker);
        setActive(true);
        super.setup();
    }

    @Override
    public void finish() {
        super.finish();
        setActive(false);
        gate.setDocker(null);
    }

    public boolean send(byte[] payload, int priority, Ship.Delegate delegate) {
        if (isActive()) {
            return gate.send(payload, priority, delegate);
        } else {
            return false;
        }
    }

    //
    //  Gate Delegate
    //

    @Override
    public void onStatusChanged(Gate gate, Gate.Status oldStatus, Gate.Status newStatus) {
        super.onStatusChanged(gate, oldStatus, newStatus);
        if (newStatus.equals(Gate.Status.CONNECTED)) {
            Messenger.Delegate delegate = getMessenger().getDelegate();
            if (delegate instanceof Server) {
                ((Server) delegate).handshake(null);
            }
        }
    }
}
