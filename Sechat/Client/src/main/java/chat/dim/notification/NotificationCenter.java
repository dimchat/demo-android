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
package chat.dim.notification;

import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

/**
 *  Notification center
 */
public final class NotificationCenter {
    private static final NotificationCenter ourInstance = new NotificationCenter();
    public static NotificationCenter getInstance() {
        return ourInstance;
    }
    private NotificationCenter() {
    }

    private Map<String, List<WeakReference>> observerMap = new Hashtable<>();

    /**
     *  Add observer with notification name
     *
     * @param observer - who will receive notification
     * @param name - notification name
     */
    public synchronized void addObserver(Observer observer, String name) {
        List<WeakReference> list = observerMap.get(name);
        if (list == null) {
            list = new Vector<>();
            observerMap.put(name, list);
        } else {
            for (WeakReference ref : list) {
                if (observer == ref.get()) {
                    // already exists
                    return;
                }
            }
        }
        list.add(new WeakReference<>(observer));
    }

    /**
     *  Remove observer for notification name
     *
     * @param observer - who will receive notification
     * @param name - notification name
     */
    public synchronized void removeObserver(Observer observer, String name) {
        List<WeakReference> list = observerMap.get(name);
        if (list == null) {
            return;
        }
        for (WeakReference ref : list) {
            if (observer == ref.get()) {
                // got it
                list.remove(ref);
                break;
            }
        }
    }

    /**
     *  Remove observer from notification center, no mather what names
     *
     * @param observer - who will receive notification
     */
    public synchronized void removeObserver(Observer observer) {
        Set<String> keys = observerMap.keySet();
        for (String name : keys) {
            removeObserver(observer, name);
        }
    }

    /**
     *  Post a notification with name
     *
     * @param name - notification name
     * @param sender - who post this notification
     */
    public void postNotification(String name, Object sender) {
        postNotification(name, sender, null);
    }

    /**
     *  Post a notification with extra info
     *
     * @param name - notification name
     * @param sender - who post this notification
     * @param userInfo - extra info
     */
    public void postNotification(String name, Object sender, Map userInfo) {
        Notification notification = new Notification(name, sender, userInfo);
        postNotification(notification);
    }

    /**
     *  Post a notification
     *
     * @param notification - notification object
     */
    private void postNotification(Notification notification) {
        // a temporary array buffer, used as a snapshot of the state of current observers
        Object[] array;

        synchronized (this) {
            List list = observerMap.get(notification.name);
            if (list == null) {
                array = null;
            } else  {
                array = list.toArray();
            }
        }
        if (array == null) {
            return;
        }

        Observer observer;
        for (Object item : array) {
            observer = (Observer) ((WeakReference) item).get();
            if (observer == null) {
                continue;
            }
            observer.onReceiveNotification(notification);
        }
    }
}
