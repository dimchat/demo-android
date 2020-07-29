/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.sechat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BackgroundThread extends Thread {

    private static final BackgroundThread ourInstance = new BackgroundThread();
    public static BackgroundThread getInstance() { return ourInstance; }
    private BackgroundThread() {
        super();
        start();
    }

    public static void run(Runnable runnable) {
        getInstance().addTask(runnable);
    }

    private final List<Runnable> tasks = new ArrayList<>();
    private final ReadWriteLock taskLock = new ReentrantReadWriteLock();

    private void addTask(Runnable runnable) {
        Lock writeLock = taskLock.writeLock();
        writeLock.lock();
        try {
            tasks.add(runnable);
        } finally {
            writeLock.unlock();
        }
    }

    private Runnable getTask() {
        Runnable task = null;
        Lock writeLock = taskLock.writeLock();
        writeLock.lock();
        try {
            if (tasks.size() > 0) {
                task = tasks.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return task;
    }

    private static void _sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Runnable task;
        while (true) {
            task = getTask();
            if (task == null) {
                // no more task, have a rest. ^_^
                _sleep(100);
                continue;
            }
            try {
                task.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
