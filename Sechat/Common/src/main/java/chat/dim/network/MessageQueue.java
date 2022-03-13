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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.port.Departure;
import chat.dim.protocol.ReliableMessage;

final class MessageQueue {

    private final List<Integer> priorities = new ArrayList<>();
    private final Map<Integer, List<MessageWrapper>> fleets = new HashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    boolean append(ReliableMessage msg, Departure ship) {
        int priority = ship.getPriority();
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            // 1. choose an array with priority
            List<MessageWrapper> queue = fleets.get(priority);
            if (queue == null) {
                // 1.1. create new array for this priority
                queue = new ArrayList<>();
                fleets.put(priority, queue);
                // 1.2. insert the priority in a sorted list
                insertPriority(priority);
            } else {
                // 1.3. check duplicated
                String signature = (String) msg.get("signature");
                ReliableMessage item;
                for (MessageWrapper wrapper : queue) {
                    item = wrapper.getMessage();
                    if (item != null && item.get("signature").equals(signature)) {
                        // duplicated message
                        return true;
                    }
                }
            }
            queue.add(new MessageWrapper(msg, ship));
        } finally {
            writeLock.unlock();
        }
        return true;
    }
    private void insertPriority(int priority) {
        int total = priorities.size();
        int index = 0, value;
        // seeking position for new priority
        for (; index < total; ++index) {
            value = priorities.get(index);
            if (value == priority) {
                // duplicated
                return;
            } else if (value > priority) {
                // got it
                break;
            }
            // current value is smaller than the new value,
            // keep going
        }
        // insert new value before the bigger one
        priorities.add(index, priority);
    }

    MessageWrapper next() {
        MessageWrapper wrapper = null;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            List<MessageWrapper> queue;
            Iterator<MessageWrapper> iterator;
            MessageWrapper item;
            for (int priority : priorities) {
                // 1. get tasks with priority
                queue = fleets.get(priority);
                if (queue == null) {
                    continue;
                }
                // 2. seeking new task in this priority
                iterator = queue.iterator();
                while (iterator.hasNext()) {
                    item = iterator.next();
                    if (item.isVirgin()) {
                        wrapper = item;
                        item.mark();  // mark sent
                        break;
                    }
                }
                if (wrapper != null) {
                    // got it
                    break;
                }
            }
        } finally {
            writeLock.unlock();
        }
        return wrapper;
    }

    private MessageWrapper eject(long now) {
        MessageWrapper wrapper = null;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            List<MessageWrapper> queue;
            Iterator<MessageWrapper> iterator;
            MessageWrapper item;
            for (int priority : priorities) {
                // 1. get tasks with priority
                queue = fleets.get(priority);
                if (queue == null) {
                    continue;
                }
                // 2. seeking new task in this priority
                iterator = queue.iterator();
                while (iterator.hasNext()) {
                    item = iterator.next();
                    if (item.getMessage() == null || item.isExpired(now)) {
                        wrapper = item;
                        iterator.remove();
                        break;
                    }
                }
                if (wrapper != null) {
                    // got it
                    break;
                }
            }
        } finally {
            writeLock.unlock();
        }
        return wrapper;
    }

    int purge() {
        int count = 0;
        long now = (new Date()).getTime();
        MessageWrapper wrapper = eject(now);
        while (wrapper != null) {
            count += 1;
            // TODO: callback for failed task?
            wrapper = eject(now);
        }
        return count;
    }
}
