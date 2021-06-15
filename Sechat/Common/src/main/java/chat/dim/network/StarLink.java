/* license: https://mit-license.org
 *
 *  Star Gate: Network Connection Module
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
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

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.tcp.ActiveConnection;

public class StarLink extends ActiveConnection {

    private final List<byte[]> outgoPackages = new ArrayList<>();
    private final ReadWriteLock outgoLock = new ReentrantReadWriteLock();

    public StarLink(String remoteHost, int remotePort, Socket connectedSocket) {
        super(remoteHost, remotePort, connectedSocket);
    }

    public StarLink(String serverHost, int serverPort) {
        super(serverHost, serverPort);
    }

    @Override
    public int send(byte[] data) {
        Lock writeLock = outgoLock.writeLock();
        writeLock.lock();
        try {
            outgoPackages.add(data);
        } finally {
            writeLock.unlock();
        }
        return data.length;
    }

    private byte[] nextOutgo() {
        byte[] data = null;
        Lock writeLock = outgoLock.writeLock();
        writeLock.lock();
        try {
            if (outgoPackages.size() > 0) {
                data = outgoPackages.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return data;
    }

    @Override
    public boolean process() {
        boolean ok = super.process();
        byte[] data = nextOutgo();
        if (data != null) {
            if (super.send(data) == data.length) {
                ok = true;
            }
        }
        return ok;
    }
}
