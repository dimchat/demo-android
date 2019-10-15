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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import chat.dim.stargate.StarStatus;

class SocketClient implements Runnable {

    private String host;
    private int port;
    private Socket socket = null;

    Fence connection = null;

    SocketClient(String host, int port) {
        super();
        this.host = host;
        this.port = port;
    }

    private int connect() {
        return connect(host, port);
    }

    private int connect(String host, int port) {
        // switch status to 'Connecting'
        connection.setStatus(StarStatus.Connecting);
        try {
            socket = new Socket(host, port);
            // switch status to 'Connected'
            connection.setStatus(StarStatus.Connected);
        } catch (IOException e) {
            e.printStackTrace();
            // switch status to 'Error'
            connection.setStatus(StarStatus.Error);
            return -1;
        }
        return 0;
    }

    private byte[] process(byte[] data, Task task) {
        if (data == null || data.length < 3) {
            return null;
        }
        if (data[0] != '{') {
            throw new IllegalArgumentException("unknown protocol: " + data);
        }
        int pos = 0;
        while (++pos < data.length) {
            if (data[pos] == '\n') {
                break;
            }
        }
        if (pos >= data.length) {
            // incomplete data
            return data;
        }
        pos += 1;
        byte[] pack;
        byte[] tail;
        if (pos == data.length) {
            pack = data;
            tail = null;
        } else {
            pack = new byte[pos];
            System.arraycopy(data, 0, pack, 0, pos);
            // next package
            tail = new byte[data.length - pos];
            System.arraycopy(data, pos, tail, 0, data.length - pos);
        }

        if (task == null) {
            connection.onReceive(pack);
        } else {
            task.onResponse(pack);
            task.onSuccess();
        }

        return tail;
    }

    private byte[] read(byte[] incomplete) {
        byte[] data;
        int length;
        try {
            InputStream inputStream = socket.getInputStream();
            length = inputStream.available();
            if (length <= 0) {
                return incomplete;
            }
            data = new byte[length];
            int count = inputStream.read(data);
            assert count == length;
        } catch (IOException e) {
            e.printStackTrace();
            return incomplete;
        }
        if (incomplete == null) {
            return data;
        }
        // merge with incomplete data
        int incompleteLength = incomplete.length;
        byte[] total = new byte[incompleteLength + length];
        System.arraycopy(incomplete, 0, total, 0, incompleteLength);
        System.arraycopy(data, 0, total, incompleteLength, length);
        return total;
    }

    private void write(byte[] data) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(data);
        } catch (IOException e) {
            e.printStackTrace();
            // TODO: socket broken, try to reconnect
            connect();
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private byte[] packRequestData(Task task) {
        // TODO: the python station support two packing format now
        //    1. NetMsg format
        //    2. Plaintext format ends with '\n'
        byte[] data = task.getRequestData();
        byte[] pack = new byte[data.length + 1];
        System.arraycopy(data, 0, pack, 0, data.length);
        pack[data.length] = '\n';
        return pack;
    }

    private byte[] packHeartbeatData() {
        // TODO: the python station support two packing format now
        //    1. NetMsg format
        //    2. Plaintext format ends with '\n'
        byte[] empty = new byte[1];
        empty[0] = '\n';
        return empty;
    }

    @Override
    public void run() {
        if (connect() < 0 || socket == null) {
            throw new NullPointerException("socket not connected");
        }
        final long HEARTBEAT_INTERVAL = 5 * 60 * 1000;
        long lastTimestamp = System.currentTimeMillis();
        long currentTimestamp;

        byte[] data = null;
        while (socket.isConnected()) {
            currentTimestamp = System.currentTimeMillis();
            // checking network pushed messages
            data = read(data);
            if (data != null) {
                data = process(data, null);
                lastTimestamp = currentTimestamp;
                continue;
            }
            // checking waiting (sending) tasks
            Task task = connection.popTask();
            if (task != null) {
                // pack and send request data
                write(packRequestData(task));
                // checking response for this task
                data = read(null);
                data = process(data, task);
                lastTimestamp = currentTimestamp;
                continue;
            }
            // checking heartbeats
            if (currentTimestamp - lastTimestamp > HEARTBEAT_INTERVAL) {
                // pack and send NOOP
                write(packHeartbeatData());
                lastTimestamp = currentTimestamp;
                continue;
            }
            // nothing to do, just sleep
            sleep(500);
        }
        connection.setStatus(StarStatus.Error);
    }
}
