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
package chat.dim.network;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.database.Database;
import chat.dim.digest.MD5;
import chat.dim.format.Hex;
import chat.dim.notification.NotificationCenter;
import chat.dim.utils.StringUtils;

public class Downloader extends Thread {
    private static final Downloader ourInstance = new Downloader();
    public static Downloader getInstance() { return ourInstance; }
    private Downloader() {
        super();
        start();
    }

    public static final String FileDownloadSuccess = "FileDownloadSuccess";
    public static final String FileDownloadFailure = "FileDownloadFailure";

    public static final String FileUploadSuccess = "FileUploadSuccess";
    public static final String FileUploadFailure = "FileUploadFailure";

    private boolean running = false;

    //
    //  download tasks
    //
    private final List<String> downloadingList = new ArrayList<>();
    private final ReentrantReadWriteLock downloadingLock = new ReentrantReadWriteLock();

    public String download(String url) {
        String filepath = check(url);
        if (filepath != null) {
            // already exists
            return filepath;
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
    private final ReentrantReadWriteLock uploadingLock = new ReentrantReadWriteLock();

    private class UploadTask {
        final byte[] data;
        final String url;
        final String filename;
        final String name;

        /**
         *  Upload data to URL with filename and variable name in form
         *
         * @param data     - file data
         * @param url      - API
         * @param filename - file name
         * @param name     - variable name in form
         */
        UploadTask(byte[] data, String url, String filename, String name) {
            this.url = url;
            this.name = name;
            this.filename = filename;
            this.data = data;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            } else if (other instanceof UploadTask) {
                UploadTask task = (UploadTask) other;
                return url.equals(task.url) && Arrays.equals(data, task.data);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(data);
        }
    }

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
                    response = httpPost(task);
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
                        nc.postNotification(FileUploadFailure, this, info);
                    } else {
                        info.put("response", response);
                        nc.postNotification(FileUploadSuccess, this, info);
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
                    filepath = httpGet(url);
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
                        nc.postNotification(FileDownloadFailure, this, info);
                    } else {
                        info.put("path", filepath);
                        nc.postNotification(FileDownloadSuccess, this, info);
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

    private static byte[] buildHTTPBody(byte[] data, String filename, String name) {
        String begin = "--4Tcjm5mp8BNiQN5YnxAAAnexqnbb3MrWjK\r\n"
                + "Content-Disposition: form-data; name=" + name + "; filename=" + filename + "\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n";
        String end = "\r\n--4Tcjm5mp8BNiQN5YnxAAAnexqnbb3MrWjK--";
        byte[] head = begin.getBytes(Charset.forName("UTF-8"));
        byte[] tail = end.getBytes(Charset.forName("UTF-8"));

        byte[] buffer = new byte[head.length + data.length + tail.length];

        System.arraycopy(head, 0, buffer, 0, head.length);
        System.arraycopy(data, 0, buffer, head.length, data.length);
        System.arraycopy(tail, 0, buffer, head.length + data.length, tail.length);
        return buffer;
    }

    private static String httpPost(UploadTask task) throws IOException {
        byte[] data = buildHTTPBody(task.data, task.filename, task.name);

        URL url = new URL(task.url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);

        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=4Tcjm5mp8BNiQN5YnxAAAnexqnbb3MrWjK");
        connection.setRequestProperty("Content-Length", String.valueOf(data.length));
        //connection.connect();

        OutputStream outputStream = connection.getOutputStream();
        outputStream.write(data);
        outputStream.flush();
        outputStream.close();

        int code = connection.getResponseCode();
        if (code == 200) {
            InputStream inputStream = connection.getInputStream();
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                sb.append(new String(buffer, 0, len));
            }
            return sb.toString();
        }
        return null;
    }

    private static String httpGet(String urlString) throws IOException {
        String filepath = null;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(false);
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.setUseCaches(true);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);
        connection.connect();
        int code = connection.getResponseCode();
        if (code == 200) {
            InputStream inputStream = connection.getInputStream();

            filepath = prepareFilePath(urlString);
            File file = new File(filepath);
            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, len);
            }

            outputStream.flush();
            outputStream.close();

            inputStream.close();
        }

        return filepath;
    }

    private static String prepareFilePath(String url) throws IOException {
        String path = getCachePath(url);
        String dir = Database.getParentDirectory(path);
        assert dir != null : "should not happen";
        File file = new File(dir);
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("failed to create directory: " + file);
        }
        return path;
    }

    // "/sdcard/chat.dim.sechat/caches/{XX}/{filename}"
    public static String getCachePath(String url) {
        String filename = StringUtils.filename(url);
        String ext = StringUtils.extension(filename);
        byte[] data = url.getBytes(Charset.forName("UTF-8"));
        if (ext == null || ext.length() == 0) {
            filename = Hex.encode(MD5.digest(data));
        } else {
            filename = Hex.encode(MD5.digest(data)) + "." + ext;
        }
        return Database.getCacheFilePath(filename);
    }

    private static String check(String url) {
        String filepath = getCachePath(url);
        File file = new File(filepath);
        if (file.exists()) {
            // already downloaded
            return filepath;
        }
        return null;
    }
}
