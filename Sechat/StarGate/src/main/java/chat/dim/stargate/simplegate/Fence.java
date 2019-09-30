/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
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
package chat.dim.stargate.simplegate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarStatus;

public class Fence implements Star {

    private StarStatus status = StarStatus.Init;
    private StarDelegate delegate;

    private SocketClient sock;
    private Thread thread;

    private List<Task> waitingList = new ArrayList<>();

    public Fence(StarDelegate messageHandler) {
        super();

        delegate = messageHandler;
    }

    void onReceive(byte[] pushData) {
        delegate.onReceive(pushData, this);
    }

    void setStatus(StarStatus gateStatus) {
        if (status == gateStatus) {
            return;
        }
        delegate.onStatusChanged(gateStatus, this);
        status = gateStatus;
    }

    Task popTask() {
        if (waitingList.size() > 0) {
            return waitingList.remove(0);
        }
        return null;
    }

    //-------- Star

    @Override
    public StarStatus getStatus() {
        return status;
    }

    @Override
    public void launch(Map<String, Object> options) {

        String host = (String) options.get("host");
        int port = (int) options.get("port");

        sock = new SocketClient(host, port);
        sock.connection = this;

        thread = new Thread(sock);
        thread.start();

    }

    @Override
    public void terminate() {
        setStatus(StarStatus.Init);

        thread.interrupt();
    }

    @Override
    public void enterBackground() {

    }

    @Override
    public void enterForeground() {

    }

    @Override
    public void send(byte[] requestData) {
        send(requestData, delegate);
    }

    @Override
    public void send(byte[] requestData, StarDelegate messageHandler) {
        Task task = new Task(requestData, messageHandler);
        task.star = this;
        waitingList.add(task);
    }
}
