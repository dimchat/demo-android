/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.crypto.SymmetricKey;
import chat.dim.digest.MD5;
import chat.dim.dkd.BaseTextContent;
import chat.dim.filesys.Paths;
import chat.dim.format.Hex;
import chat.dim.format.JSONMap;
import chat.dim.model.MessageDataSource;
import chat.dim.network.FtpServer;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Content;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;
import chat.dim.skywalker.Runner;
import chat.dim.threading.Daemon;
import chat.dim.type.Pair;
import chat.dim.type.WeakMap;
import chat.dim.utils.Log;

public class Emitter extends Runner implements Observer {

    public Emitter() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.FileUploadSuccess);
        nc.addObserver(this, NotificationNames.FileUploadFailure);
    }

    @Override
    protected void finalize() throws Throwable {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.FileUploadFailure);
        nc.removeObserver(this, NotificationNames.FileUploadSuccess);
        super.finalize();
    }

    public void sendText(String text, ID receiver) {
        TextContent content = new BaseTextContent(text);
        sendContent(content, receiver);
    }

    public void sendImage(byte[] jpeg, byte[] thumbnail, ID receiver) {
        assert jpeg != null && jpeg.length > 0 : "image data empty";
        String filename = Hex.encode(MD5.digest(jpeg)) + ".jpeg";
        ImageContent content = FileContent.image(filename, jpeg);
        // add image data length & thumbnail into message content
        content.put("length", jpeg.length);
        content.setThumbnail(thumbnail);
        sendContent(content, receiver);
    }

    public void sendVoice(byte[] mp4, int duration, ID receiver) {
        assert mp4 != null && mp4.length > 0 : "voice data empty";
        String filename = Hex.encode(MD5.digest(mp4)) + ".mp4";
        AudioContent content = FileContent.audio(filename, mp4);
        // add voice data length & duration into message content
        content.put("length", mp4.length);
        content.put("duration", duration);
        sendContent(content, receiver);
    }

    private void sendContent(Content content, ID receiver) {
        assert receiver != null : "receiver should not empty";
        GlobalVariable shared = GlobalVariable.getInstance();
        Pair<InstantMessage, ReliableMessage> result;
        result = shared.messenger.sendContent(null, receiver, content, 0);
        assert result.first != null : "failed to pack instant message: " + receiver;
        // save instant message
        MessageDataSource mds = MessageDataSource.getInstance();
        mds.saveInstantMessage(result.first);
    }

    public void sendFileContentMessage(InstantMessage iMsg, SymmetricKey password) {
        FileContent content = (FileContent) iMsg.getContent();
        // 1. save origin file data
        byte[] data = content.getData();
        String filename = content.getFilename();
        FtpServer ftp = FtpServer.getInstance();
        int len = ftp.saveFileData(data, filename);
        if (len != data.length) {
            Log.error("failed to save file data (len=" + data.length + "): " + filename);
            return;
        }
        // 2. save instant message without file data
        content.setData(null);
        MessageDataSource mds = MessageDataSource.getInstance();
        mds.saveInstantMessage(iMsg);
        // 3. add upload task with encrypted data
        byte[] encrypted = password.encrypt(data);
        String ext = Paths.extension(filename);
        filename = Hex.encode(MD5.digest(encrypted));
        if (ext != null && ext.length() > 0) {
            filename = filename + "." + ext;
        }
        Log.info("task filename: " + content.getFilename() + " -> " + filename);
        Task task = new Task(filename, encrypted, iMsg);
        addTask(task);
    }

    private static class Task {
        static final long EXPIRES = 300 * 1000;  // 5 minutes

        final String filename;
        final byte[] encrypted;
        final InstantMessage iMsg;
        long time = 0;
        Task(String filename, byte[] encrypted, InstantMessage iMsg) {
            this.filename = filename;
            this.encrypted = encrypted;
            this.iMsg = iMsg;
        }
    }
    private final List<Task> tasks = new ArrayList<>();
    private final Map<String, Task> map = new WeakMap<>();  // filename => task
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    private void addTask(Task item) {
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            tasks.add(item);
            map.put(item.filename, item);
        } finally {
            writeLock.unlock();
        }
    }
    private Task getTask(String filename) {
        Task item;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            item = map.get(filename);
        } finally {
            writeLock.unlock();
        }
        return item;
    }
    private Task nextTask() {
        Task next = null;
        long now = System.currentTimeMillis();
        long expired = now - Task.EXPIRES;

        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            Iterator<Task> iterator = tasks.iterator();
            Task item;
            while (iterator.hasNext()) {
                item = iterator.next();
                if (item.time == 0) {
                    // got it
                    item.time = now;
                    next = item;
                    break;
                } else if (item.time < expired) {
                    // expired, remove it
                    map.remove(item.filename);
                    iterator.remove();
                }
            }
        } finally {
            writeLock.unlock();
        }
        return next;
    }

    private final Daemon daemon = new Daemon(this);

    public Emitter start() {
        daemon.start();
        return this;
    }

    @Override
    public void stop() {
        super.stop();
        daemon.stop();
    }

    @Override
    public boolean process() {
        Task next = nextTask();
        if (next == null) {
            // nothing to do now, return false to have a rest
            return false;
        }
        try {
            byte[] encrypted = next.encrypted;
            String filename = next.filename;
            ID sender = next.iMsg.getSender();
            Log.info("uploading file: " + filename + ", sender: " + sender);
            FtpServer ftp = FtpServer.getInstance();
            ftp.uploadEncryptedData(encrypted, filename, sender);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map<String, Object> userInfo = notification.userInfo;

        String filename = null;
        if (name == null) {
            Log.error("notification error: " + notification);
        } else if (name.equals(NotificationNames.FileUploadSuccess)) {
            String response = (String) userInfo.get("response");
            if (response == null) {
                Log.error("response not found: " + userInfo);
            } else {
                Map res = JSONMap.decode(response);
                filename = (String) res.get("filename");
                String url = (String) res.get("url");
                if (url == null) {
                    Log.error("url not found: " + response);
                } else {
                    Task task = getTask(filename);
                    if (task == null) {
                        Log.error("failed to get task: " + filename + ", url: " + url);
                    } else {
                        Log.info("get task for file: " + filename + ", url: " + url);
                        // file data uploaded to FTP server, replace it with download URL
                        // and send the content to station
                        InstantMessage iMsg = task.iMsg;
                        FileContent content = (FileContent) iMsg.getContent();
                        //content.setData(null);
                        content.setURL(url);
                        GlobalVariable shared = GlobalVariable.getInstance();
                        shared.messenger.sendInstantMessage(iMsg, 0);
                        // set expired to be removed
                        task.time = -1;
                        return;
                    }
                }
            }
        } else if (name.equals(NotificationNames.FileUploadFailure)) {
            String response = (String) userInfo.get("response");
            if (response == null) {
                Log.error("response not found: " + userInfo);
            } else {
                Map res = JSONMap.decode(response);
                filename = (String) res.get("filename");
            }
        } else {
            Log.warning("notification name error: " + name);
        }

        if (filename != null) {
            Log.error("upload failed: " + filename);
            Task task = getTask(filename);
            if (task != null) {
                // set expired to be removed
                task.time = -1;
            }
        }
    }
}