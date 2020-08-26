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
package chat.dim.http;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.digest.MD5;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;
import chat.dim.format.Hex;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;

public class HTTPClient extends Thread {

    private static final HTTPClient ourInstance = new HTTPClient();
    public static HTTPClient getInstance() { return ourInstance; }
    private HTTPClient() {
        super();
        start();
    }

    private boolean running = false;

    // "/sdcard/chat.dim.sechat/caches/{XX}/{filename}"
    public static String getCachePath(String url) throws IOException {
        String filename = Paths.getFilename(url);
        int pos = filename.indexOf(".");
        if (pos != 32) {
            // filename not hashed by MD5, hash the whole URL instead
            String ext = Paths.getExtension(filename);
            if (ext == null || ext.length() == 0) {
                ext = "tmp";
            }
            byte[] data = url.getBytes(Charset.forName("UTF-8"));
            filename = Hex.encode(MD5.digest(data)) + "." + ext;

        }
        return ExternalStorage.getCacheFilePath(filename);
    }

    //
    //  download tasks
    //
    private final List<String> downloadingList = new ArrayList<>();
    private final ReadWriteLock downloadingLock = new ReentrantReadWriteLock();

    public String download(String url) {
        String path;
        try {
            path = getCachePath(url);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        if (ExternalStorage.exists(path)) {
            // already downloaded
            return path;
        }

        // add task to download
        Lock writeLock = downloadingLock.writeLock();
        writeLock.lock();
        try {
            // check duplicated task
            if (!downloadingList.contains(url)) {
                downloadingList.add(url);
            }
        } finally {
            writeLock.unlock();
        }
        return null;
    }

    private String getDownloadTask() {
        String url = null;
        Lock writeLock = downloadingLock.writeLock();
        writeLock.lock();
        try {
            if (downloadingList.size() > 0) {
                url = downloadingList.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return url;
    }

    //
    //  upload tasks
    //
    private final List<UploadTask> uploadingList = new ArrayList<>();
    private final ReadWriteLock uploadingLock = new ReentrantReadWriteLock();

    public void upload(byte[] data, String url, String filename, String name) {
        UploadTask task = new UploadTask(data, url, filename, name);
        // add task to upload
        Lock writeLock = uploadingLock.writeLock();
        writeLock.lock();
        try {
            // check duplicated task
            if (!uploadingList.contains(task)) {
                uploadingList.add(task);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private UploadTask getUploadTask() {
        UploadTask task = null;
        Lock writeLock = uploadingLock.writeLock();
        writeLock.lock();
        try {
            if (uploadingList.size() > 0) {
                task = uploadingList.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return task;
    }

    //
    //  Download/upload in background thread
    //

    private static void _sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void start() {
        running = true;
        super.start();
    }

    public void close() {
        running = false;
    }

    @Override
    public void run() {
        NotificationCenter nc = NotificationCenter.getInstance();

        UploadTask task;
        String response;

        String url;
        String filepath;

        _sleep(1000);
        while (running) {
            // 1. get one upload task
            task = getUploadTask();
            if (task != null) {
                // 1.1. try to upload
                try {
                    response = Request.post(task);
                } catch (IOException e) {
                    e.printStackTrace();
                    response = null;
                    _sleep(1000);
                }
                // 1.2. post notification
                try {
                    Map<String, Object> info = new HashMap<>();
                    info.put("URL", task.url);
                    info.put("filename", task.filename);
                    info.put("name", task.name);
                    info.put("data", task.data);
                    if (response == null) {
                        nc.postNotification(NotificationNames.FileUploadFailure, this, info);
                    } else {
                        info.put("response", response);
                        nc.postNotification(NotificationNames.FileUploadSuccess, this, info);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continue;
            }

            // 2. get one download task
            url = getDownloadTask();
            if (url != null) {
                // 2.1. try to download
                try {
                    filepath = Request.get(url);
                } catch (IOException e) {
                    e.printStackTrace();
                    filepath = null;
                    _sleep(1000);
                }
                // 2.2. post notification
                try {
                    Map<String, Object> info = new HashMap<>();
                    info.put("URL", url);
                    if (filepath == null) {
                        nc.postNotification(NotificationNames.FileDownloadFailure, this, info);
                    } else {
                        info.put("path", filepath);
                        nc.postNotification(NotificationNames.FileDownloadSuccess, this, info);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                continue;
            }

            // no job to do now, have a rest. ^_^
            _sleep(500);
        }
    }
}
