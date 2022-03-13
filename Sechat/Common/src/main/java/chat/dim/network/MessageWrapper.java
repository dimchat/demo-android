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

import java.util.Date;
import java.util.List;

import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Docker;
import chat.dim.protocol.ReliableMessage;
import chat.dim.utils.Log;

final class MessageWrapper implements Departure {

    public static int EXPIRES = 600 * 1000;  // 10 minutes

    private long timestamp;
    private ReliableMessage msg;

    private final Departure departure;

    MessageWrapper(ReliableMessage rMsg, Departure outgo) {
        super();
        timestamp = 0;
        msg = rMsg;
        departure = outgo;
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

    boolean isExpired(long now) {
        if (timestamp < 0) {
            return true;
        }
        if (timestamp == 0) {
            return false;
        }
        long expired = timestamp + EXPIRES;
        return now > expired;
    }

    ReliableMessage getMessage() {
        return msg;
    }

    // Departure Ship

    @Override
    public int getPriority() {
        return departure.getPriority();
    }

    @Override
    public int getRetries() {
        return departure.getRetries();
    }

    @Override
    public boolean isTimeout(long now) {
        return departure.isTimeout(now);
    }

    @Override
    public List<byte[]> getFragments() {
        return departure.getFragments();
    }

    @Override
    public boolean checkResponse(Arrival response) {
        return departure.checkResponse(response);
    }

    @Override
    public Object getSN() {
        return departure.getSN();
    }

    @Override
    public boolean isFailed(long now) {
        return departure.isFailed(now);
    }

    @Override
    public boolean update(long now) {
        return departure.update(now);
    }

    //
    //  Callback
    //

    // message appended to outgoing queue
    public void onAppended() {
        // this message was assigned to the worker of StarGate,
        // update sent time
        timestamp = (new Date()).getTime();
    }

    // gate error, failed to append
    public void onGateError(Error error) {
        // failed
        timestamp = -1;
    }

    public void onSent(Docker docker) {
        Log.info("message sent: " + docker + " -> " + departure);
        // success, remove message
        msg = null;
    }

    public void onFailed(Throwable error, Docker docker) {
        Log.error("connection error: " + error.getLocalizedMessage() + ", " + docker);
        // failed
        timestamp = -1;
    }
}
