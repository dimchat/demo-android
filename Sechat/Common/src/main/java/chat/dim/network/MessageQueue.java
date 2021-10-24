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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.protocol.ReliableMessage;

final class MessageQueue {

    private final List<MessageWrapper> queue = new ArrayList<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    boolean append(ReliableMessage msg) {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            queue.add(new MessageWrapper(msg));
        } finally {
            writeLock.unlock();
        }
        return true;
    }

    MessageWrapper shift() {
        MessageWrapper wrapper = null;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            if (queue.size() > 0) {
                wrapper = queue.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return wrapper;
    }

    MessageWrapper next() {
        MessageWrapper wrapper = null;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            for (MessageWrapper item : queue) {
                if (item.isVirgin()) {
                    wrapper = item;
                    item.mark();  // mark sent
                    break;
                }
            }
        } finally {
            writeLock.unlock();
        }
        return wrapper;
    }

    MessageWrapper eject() {
        MessageWrapper wrapper = null;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            for (MessageWrapper item : queue) {
                if (item.getMessage() == null || item.isFailed()) {
                    wrapper = item;
                    queue.remove(item);
                    break;
                }
            }
        } finally {
            writeLock.unlock();
        }
        return wrapper;
    }
}
