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

import java.net.SocketAddress;
import java.util.Date;

import chat.dim.Messenger;
import chat.dim.mtp.StreamArrival;
import chat.dim.mtp.StreamDeparture;
import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Gate;
import chat.dim.protocol.ReliableMessage;

final class MessageWrapper implements StarDelegate, Messenger.Callback {

    private long timestamp;
    private ReliableMessage msg;

    MessageWrapper(ReliableMessage rMsg) {
        super();
        timestamp = 0;
        msg = rMsg;
    }

    ReliableMessage getMessage() {
        return msg;
    }

    void mark() {
        timestamp = 1;
    }
    void fail() {
        timestamp = -1;
    }

    boolean isVirgin() {
        return timestamp == 0;
    }

    boolean isFailed() {
        if (timestamp < 0) {
            return true;
        }
        if (timestamp == 0) {
            return false;
        }
        Date now = new Date();
        long delta = now.getTime() - timestamp;
        return delta > BaseSession.EXPIRES;
    }

    //
    //  Gate Delegate
    //

    @Override
    public void onStatusChanged(Gate.Status oldStatus, Gate.Status newStatus, SocketAddress remote, Gate gate) {

    }

    @Override
    public void onReceived(Arrival income, SocketAddress source, SocketAddress destination, Gate gate) {

    }

    @Override
    public void onSent(Departure outgo, SocketAddress source, SocketAddress destination, Gate gate) {
        // success, remove message
        msg = null;
    }

    @Override
    public void onError(Error error, Departure outgo, SocketAddress source, SocketAddress destination, Gate gate) {
        // failed
        timestamp = -1;
    }

    //
    //  Messenger Callback
    //

    @Override
    public void onFinished(Object result, Error error) {
        if (error == null) {
            // this message was assigned to the worker of StarGate,
            // update sent time
            timestamp = (new Date()).getTime();
        } else {
            // failed
            timestamp = -1;
        }
    }
}
