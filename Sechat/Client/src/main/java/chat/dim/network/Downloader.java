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
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.digest.MD5;
import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Hex;
import chat.dim.notification.NotificationCenter;

public class Downloader extends Thread {
    private static final Downloader ourInstance = new Downloader();
    public static Downloader getInstance() { return ourInstance; }
    private Downloader() {
        super();
        start();
    }

    public static final String FileDownloadSuccess = "FileDownloadSuccess";
    public static final String FileDownloadFailure = "FileDownloadFailure";

    private boolean running = false;

    private final List<String> waitingList = new ArrayList<>();
    private final ReentrantReadWriteLock waitingLock = new ReentrantReadWriteLock();

    public String download(String url) {
        String filepath = check(url);
        if (filepath != null) {
            // already exists
            return filepath;
        }
        // add task to download
        addTask(url);
        return null;
    }

    public void addTask(String url) {
        Lock writeLock = waitingLock.writeLock();
        writeLock.lock();
        try {
            // check duplicated task
            if (!waitingList.contains(url)) {
                waitingList.add(url);
            }
        } finally {
            writeLock.unlock();
        }
    }

    private String getTask() {
        String url = null;
        Lock writeLock = waitingLock.writeLock();
        writeLock.lock();
        try {
            if (waitingList.size() > 0) {
                url = waitingList.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return url;
    }

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
        String url;
        String filepath;

        _sleep(1000);
        while (running) {
            // 1. get one job
            url = getTask();
            if (url == null) {
                // no job to do now, have a rest. ^_^
                _sleep(500);
                continue;
            }
            // 2. try to download
            try {
                filepath = http_get(url);
            } catch (IOException e) {
                e.printStackTrace();
                filepath = null;
                _sleep(2000);
            }
            // 3. post notification
            try {
                notice(url, filepath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void notice(String url, String filepath) {
        Map<String, Object> info = new HashMap<>();
        info.put("URL", url);
        if (filepath == null) {
            NotificationCenter.getInstance().postNotification(FileDownloadFailure, this, info);
        } else {
            info.put("path", filepath);
            NotificationCenter.getInstance().postNotification(FileDownloadSuccess, this, info);
        }
    }

    private static String http_get(String urlString) throws IOException {
        String filepath = null;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(false);
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.setUseCaches(true);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(3000);
        connection.connect();
        int code = connection.getResponseCode();
        if (code == 200) {
            InputStream inputStream = connection.getInputStream();

            filepath = getFilepath(urlString);
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

    private static String getFilepath(String url) throws IOException {
        byte[] data = url.getBytes(Charset.forName("UTF-8"));
        data = MD5.digest(data);
        String filename = Hex.encode(data);
        String ext = getFileExt(url);
        String dir = ExternalStorage.root + ExternalStorage.separator + "caches";
        File file = new File(dir);
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("failed to create directory: " + file);
        }
        return dir + ExternalStorage.separator + filename + "." + ext;
    }

    private static String getFileExt(String url) {
        int pos;
        pos = url.indexOf("?");
        if (pos > 0) {
            url = url.substring(0, pos);
        }
        pos = url.indexOf("#");
        if (pos > 0) {
            url = url.substring(0, pos);
        }
        pos = url.lastIndexOf(".");
        if (pos > 0) {
            return url.substring(pos + 1);
        }
        return "tmp";
    }

    private static String check(String url) {
        try {
            String filepath = getFilepath(url);
            File file = new File(filepath);
            if (file.exists()) {
                // already downloaded
                return filepath;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
