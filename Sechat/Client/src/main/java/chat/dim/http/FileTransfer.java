/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.http;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.DecryptKey;
import chat.dim.digest.MD5;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.LocalCache;
import chat.dim.filesys.Paths;
import chat.dim.format.Hex;
import chat.dim.format.UTF8;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.utils.Log;

public enum FileTransfer implements UploadDelegate, DownloadDelegate {

    INSTANCE;

    public static FileTransfer getInstance() {
        return INSTANCE;
    }

    // upload API
    public String api = "https://sechat.dim.chat/{ID}/upload?md5={MD5}&salt={SALT}";
    // upload key (hex)
    public String secret = "12345678";

    private final HTTPClient http;

    FileTransfer() {
        http = new HTTPClient() {
            @Override
            protected void cleanup() {
                // clean expired temporary files for upload/download
                FileTransfer ftp = FileTransfer.getInstance();
                ftp.cleanup();
            }
        };
        http.start();
    }

    /**
     *  Upload avatar image data for user
     *
     * @param data     - image data
     * @param filename - image filename ('avatar.jpg')
     * @param sender   - user ID
     * @param delegate - callback
     * @return remote URL if same file uploaded before
     * @throws IOException on failed to create temporary file
     */
    public URL uploadAvatar(byte[] data, String filename, ID sender, UploadDelegate delegate) throws IOException {
        filename = getFilename(data, Paths.filename(filename));
        LocalCache cache = LocalCache.getInstance();
        String path = cache.getAvatarFilePath(filename);
        return upload(data, path, "avatar", sender, delegate);
    }

    /**
     *  Upload encrypted file data for user
     *
     * @param data     - encrypted data
     * @param filename - data file name ('voice.mp4')
     * @param sender   - user ID
     * @param delegate - callback
     * @return remote URL if same file uploaded before
     * @throws IOException on failed to create temporary file
     */
    public URL uploadEncryptData(byte[] data, String filename, ID sender, UploadDelegate delegate) throws IOException {
        filename = getFilename(data, Paths.filename(filename));
        LocalCache cache = LocalCache.getInstance();
        String path = cache.getUploadFilePath(filename);
        return upload(data, path, "file", sender, delegate);
    }

    private URL upload(byte[] data, String path, String var, ID sender, UploadDelegate delegate) throws IOException {
        URL url = new URL(api);
        byte[] key = Hex.decode(secret);
        if (delegate == null) {
            delegate = this;
        }
        return http.upload(url, key, data, path, var, sender, delegate);
    }

    private static boolean isEncoded(String filename, String ext) {
        if (ext != null) {
            filename = filename.substring(0, filename.length() - ext.length() - 1);
        }
        return filename.length() == 32 && filename.matches("^[0-9A-Fa-f]+$");
    }

    private String getFilename(byte[] data, String filename) {
        // split file extension
        String ext = Paths.extension(filename);
        if (isEncoded(filename, ext)) {
            // already encoded
            return filename;
        }
        // get filename from data
        filename = Hex.encode(MD5.digest(data));
        if (ext == null) {
            return filename;
        }
        return filename + "." + ext;
    }

    private String getFilename(URL url) {
        String urlString = url.toString();
        String filename = Paths.filename(urlString);
        byte[] data = UTF8.encode(urlString);
        return getFilename(data, filename);
    }

    /**
     *  Download avatar image file
     *
     * @param url      - avatar URL
     * @param delegate - callback
     * @return local path if same file downloaded before
     */
    public String downloadAvatar(URL url, DownloadDelegate delegate) {
        String filename = getFilename(url);
        LocalCache cache = LocalCache.getInstance();
        String path = cache.getAvatarFilePath(filename);
        if (delegate == null) {
            delegate = this;
        }
        return http.download(url, path, delegate);
    }

    /**
     *  Download encrypted file data for user
     *
     * @param url      - relay URL
     * @param delegate - callback
     * @return temporary path if same file downloaded before
     */
    public String downloadEncryptedData(URL url, DownloadDelegate delegate) {
        String filename = getFilename(url);
        LocalCache cache = LocalCache.getInstance();
        String path = cache.getDownloadFilePath(filename);
        if (delegate == null) {
            delegate = this;
        }
        return http.download(url, path, delegate);
    }

    //
    //  Decryption
    //  ~~~~~~~~~~
    //
    //  1. get 'filename' from file content and call 'loadCachedFileData(filename)',
    //     if not null, means this file is already downloaded an decrypted;
    //
    //  2. get 'URL' from file content and call 'downloadEncryptedData()',
    //     if not null, means this file is already downloaded but not decrypted yet,
    //     this step will get a temporary path for encrypted data, continue step 3;
    //     if the return path is null, then let the delegate waiting for response;
    //
    //  3. get 'password' from file content and call 'decryptFileData(path, password)',
    //     this step will get the decrypted file data, you should save it to cache path
    //     by calling 'cacheFileData(data, filename)', notice that this filename is in
    //     hex format by hex(md5(data)), which is the same string with content.filename.
    //

    /**
     *  Decrypt temporary file with password from received message
     *
     * @param path     - temporary path
     * @param password - symmetric key
     * @return decrypted data
     */
    public byte[] decryptFileData(String path, DecryptKey password) {
        byte[] data = loadDownloadedFileData(path);
        if (data == null) {
            Log.warning("failed to load temporary file: " + path);
            return null;
        }
        return password.decrypt(data);
    }

    private byte[] loadDownloadedFileData(String filename) {
        String path = getDownloadFilePath(filename);
        if (Paths.exists(path)) {
            try {
                return ExternalStorage.loadBinary(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     *  Load cached file with name (or path)
     *
     * @param filename - cache file name
     * @return decrypted data
     */
    public byte[] loadCachedFileData(String filename) {
        String path = getCacheFilePath(filename);
        if (Paths.exists(path)) {
            try {
                return ExternalStorage.loadBinary(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     *  Save cache file with name (or path)
     *
     * @param data     - decrypted data
     * @param filename - cache file name
     * @return data length
     */
    public int cacheFileData(byte[] data, String filename) {
        String path = getCacheFilePath(filename);
        try {
            return ExternalStorage.saveBinary(data, path);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private String getCacheFilePath(String filename) {
        if (filename.contains(File.separator)) {
            // full path?
            return filename;
        } else {
            // relative path?
            LocalCache cache = LocalCache.getInstance();
            return cache.getCacheFilePath(filename);
        }
    }

    private String getDownloadFilePath(String filename) {
        if (filename.contains(File.separator)) {
            // full path?
            return filename;
        } else {
            // relative path?
            LocalCache cache = LocalCache.getInstance();
            return cache.getDownloadFilePath(filename);
        }
    }

    /**
     *  Get entity file path: "/sdcard/chat.dim.sechat/mkm/{XX}/{YY}/{address}/{filename}"
     *
     * @param entity   - user or group ID
     * @param filename - entity file name
     * @return entity file path
     */
    public String getEntityFilePath(ID entity, String filename) {
        String dir = getEntityDirectory(entity.getAddress());
        return Paths.append(dir, filename);
    }

    private String getEntityDirectory(Address address) {
        String string = address.toString();
        String dir = getEntityDirectory();
        String xx = string.substring(0, 2);
        String yy = string.substring(2, 4);
        return Paths.append(dir, xx, yy, string);
    }

    private String getEntityDirectory() {
        LocalCache cache = LocalCache.getInstance();
        return Paths.append(cache.getRoot(), "mkm");
    }

    /**
     *  Remove expired files in the temporary directory
     */
    public void cleanup() {
        long now = System.currentTimeMillis();
        LocalCache cache = LocalCache.getInstance();
        //cleanup(cache.getCachesDirectory(), now - CACHES_EXPIRES);
        ExternalStorage.cleanup(cache.getTemporaryDirectory(), now - TEMPORARY_EXPIRES);
    }
    //public static long CACHES_EXPIRES = 365 * 24 * 3600 * 1000L;
    public static long TEMPORARY_EXPIRES = 7 * 24 * 3600 * 1000L;

    //-------- Upload Delegate

    @Override
    public void onUploadSuccess(UploadRequest request, URL url) {
        Log.info("onUploadSuccess: " + request + ", url: " + url);
        Map<String, Object> response = new HashMap<>();
        response.put("URL", url);
        Map<String, Object> info = new HashMap<>();
        info.put("response", response);
        // request info
        info.put("api", request.url);
        info.put("name", request.name);
        if (request instanceof UploadTask) {
            UploadTask task = (UploadTask) request;
            info.put("filename", task.filename);
        } else {
            info.put("path", request.path);
            info.put("sender", request.sender);
        }
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileUploadSuccess, this, info);
    }

    @Override
    public void onUploadFailed(UploadRequest request, IOException error) {
        Log.error("onUploadFailed: " + request + ", error: " + error);
        Map<String, Object> info = new HashMap<>();
        info.put("error", error);
        // request info
        info.put("api", request.url);
        info.put("name", request.name);
        if (request instanceof UploadTask) {
            UploadTask task = (UploadTask) request;
            info.put("filename", task.filename);
        } else {
            info.put("path", request.path);
            info.put("sender", request.sender);
        }
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileUploadFailure, this, info);
    }

    @Override
    public void onUploadError(UploadRequest request, IOError error) {
        Log.error("onUploadError: " + request + ", error: " + error);
        Map<String, Object> info = new HashMap<>();
        info.put("error", error);
        // request info
        info.put("api", request.url);
        info.put("name", request.name);
        if (request instanceof UploadTask) {
            UploadTask task = (UploadTask) request;
            info.put("filename", task.filename);
        } else {
            info.put("path", request.path);
            info.put("sender", request.sender);
        }
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileUploadFailure, this, info);
    }

    //-------- Download Delegate

    @Override
    public void onDownloadSuccess(DownloadRequest request, String path) {
        Log.info("onDownloadSuccess: " + request + ", path: " + path);
        Map<String, Object> info = new HashMap<>();
        info.put("url", request.url);
        info.put("path", request.path);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileDownloadSuccess, this, info);
    }

    @Override
    public void onDownloadFailed(DownloadRequest request, IOException error) {
        Log.info("onDownloadFailed: " + request + ", error: " + error);
        Map<String, Object> info = new HashMap<>();
        info.put("url", request.url);
        info.put("path", request.path);
        info.put("error", error);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileDownloadFailure, this, info);
    }

    @Override
    public void onDownloadError(DownloadRequest request, IOError error) {
        Log.info("onDownloadFailed: " + request + ", error: " + error);
        Map<String, Object> info = new HashMap<>();
        info.put("url", request.url);
        info.put("path", request.path);
        info.put("error", error);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileDownloadFailure, this, info);
    }
}
