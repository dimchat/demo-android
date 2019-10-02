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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private Map<String, List<Observer>> observerMap = new HashMap<>();

    /**
     *  Add observer with notification name
     *
     * @param observer - who will receive notification
     * @param name - notification name
     */
    public void addObserver(Observer observer, String name) {
        List<Observer> list = observerMap.get(name);
        if (list == null) {
            list = new ArrayList<>();
            observerMap.put(name, list);
        } else if (list.contains(observer)) {
            return;
        }
        list.add(observer);
    }

    /**
     *  Remove observer for notification name
     *
     * @param observer - who will receive notification
     * @param name - notification name
     */
    public void removeObserver(Observer observer, String name) {
        List<Observer> list = observerMap.get(name);
        if (list == null) {
            return;
        }
        list.remove(observer);
    }

    /**
     *  Remove observer from notification center, no mather what names
     *
     * @param observer - who will receive notification
     */
    public void removeObserver(Observer observer) {
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
    public void postNotification(Notification notification) {
        List<Observer> list = observerMap.get(notification.name);
        if (list == null) {
            return;
        }
        for (Observer observer : list) {
            observer.onReceiveNotification(notification);
        }
    }
}
