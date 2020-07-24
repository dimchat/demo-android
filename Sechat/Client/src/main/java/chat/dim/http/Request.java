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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

import chat.dim.database.Database;

class Request {

    //
    //  Cache file
    //

    private static String prepareFilePath(String url) throws IOException {
        String path = HTTPClient.getCachePath(url);
        String dir = Database.getParentDirectory(path);
        assert dir != null : "should not happen";
        File file = new File(dir);
        if (!file.exists() && !file.mkdirs()) {
            throw new IOException("failed to create directory: " + file);
        }
        return path;
    }

    //
    //  Request
    //

    static String get(String urlString) throws IOException {
        String filepath = null;

        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(false);
        connection.setDoInput(true);
        connection.setRequestMethod("GET");
        connection.setUseCaches(true);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);
        //connection.connect();

        int code = connection.getResponseCode();
        if (code == 200) {
            try (InputStream inputStream = connection.getInputStream()) {
                filepath = prepareFilePath(urlString);
                File file = new File(filepath);
                try (FileOutputStream outputStream = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024];
                    int len;
                    while ((len = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.flush();
                }
            }
        }
        //connection.disconnect();

        return filepath;
    }

    static String post(UploadTask task) throws IOException {
        String response = null;

        byte[] data = buildHTTPBody(task.data, task.filename, task.name);

        URL url = new URL(task.url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setUseCaches(false);
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(5000);

        connection.setRequestProperty("Content-Type", CONTENT_TYPE);
        connection.setRequestProperty("Content-Length", String.valueOf(data.length));
        //connection.connect();

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(data);
            outputStream.flush();
        }

        int code = connection.getResponseCode();
        if (code == 200) {
            try (InputStream inputStream = connection.getInputStream()) {
                StringBuilder sb = new StringBuilder();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    sb.append(new String(buffer, 0, len));
                }
                response = sb.toString();
            }
        }
        //connection.disconnect();

        return response;
    }

    private static byte[] buildHTTPBody(byte[] data, String filename, String name) {
        String begin = String.format(BEGIN, name, filename);
        byte[] head = begin.getBytes(Charset.forName("UTF-8"));

        byte[] buffer = new byte[head.length + data.length + TAIL.length];

        System.arraycopy(head, 0, buffer, 0, head.length);
        System.arraycopy(data, 0, buffer, head.length, data.length);
        System.arraycopy(TAIL, 0, buffer, head.length + data.length, TAIL.length);
        return buffer;
    }

    private static final String BOUNDARY = "4Tcjm5mp8BNiQN5YnxAAAnexqnbb3MrWjK";

    private static final String CONTENT_TYPE = "multipart/form-data; boundary=" + BOUNDARY;

    private static final String BEGIN = "--" + BOUNDARY + "\r\n"
            + "Content-Disposition: form-data; name=%s; filename=%s\r\n"
            + "Content-Type: application/octet-stream\r\n\r\n";
    private static final String END = "\r\n--" + BOUNDARY + "--";
    private static final byte[] TAIL = END.getBytes(Charset.forName("UTF-8"));
}
