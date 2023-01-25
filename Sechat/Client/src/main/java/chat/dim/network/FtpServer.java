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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import chat.dim.crypto.SymmetricKey;
import chat.dim.digest.MD5;
import chat.dim.filesys.EntityStorage;
import chat.dim.filesys.LocalCache;
import chat.dim.filesys.Paths;
import chat.dim.format.Hex;
import chat.dim.http.DownloadTask;
import chat.dim.http.HTTPClient;
import chat.dim.http.HTTPDelegate;
import chat.dim.http.UploadTask;
import chat.dim.model.Configuration;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.type.MutableData;
import chat.dim.utils.Log;

public final class FtpServer implements HTTPDelegate {
    private static final FtpServer ourInstance = new FtpServer();
    public static FtpServer getInstance() { return ourInstance; }
    private FtpServer() {
        super();
    }

    private final Configuration config = Configuration.getInstance();

    private static byte[] generateSalt() {
        byte[] salt = new byte[16];
        Random random = new Random();
        random.nextBytes(salt);
        return salt;
    }
    private static byte[] getDigest(byte[] data, byte[] secret, byte[] salt) {
        MutableData concat = new MutableData(data.length + secret.length + salt.length);
        concat.append(data);
        concat.append(secret);
        concat.append(salt);
        return MD5.digest(concat.getBytes());
    }

    //
    //  Avatar
    //

    public String uploadAvatar(byte[] imageData, ID identifier) {

        byte[] secret = config.getMD5Secret();
        byte[] salt = generateSalt();
        byte[] digest = getDigest(imageData, secret, salt);

        String filename = Hex.encode(MD5.digest(imageData)) + ".jpeg";

        // upload to CDN
        String upload = config.getUploadURL();
        assert upload != null : "upload API error";
        upload = upload.replaceAll("\\{ID\\}", identifier.getAddress().toString());
        upload = upload.replaceAll("\\{MD5\\}", Hex.encode(digest));
        upload = upload.replaceAll("\\{SALT\\}", Hex.encode(salt));

        HTTPClient http = HTTPClient.getInstance();
        http.upload(upload, "avatar", filename, imageData);

        // build download URL
        String download = config.getAvatarURL();
        assert download != null : "download API error";
        download = download.replaceAll("\\{ID\\}", identifier.getAddress().toString()).replaceAll("\\{filename\\}", filename);

        try {
            // store in user's directory
            String path = EntityStorage.getEntityFilePath(identifier, "avatar.jpeg");
            EntityStorage.saveBinary(imageData, path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // save a copy to cache directory
        saveFileData(imageData, filename);

        return download;
    }

    public String downloadAvatar(String url, ID identifier) {
        String path;

        if (url != null) {
            HTTPClient httpClient = HTTPClient.getInstance();
            path = httpClient.download(url);
            if (path != null) {
                return path;
            }
        }

        path = EntityStorage.getEntityFilePath(identifier, "avatar.jpeg");
        if (Paths.exists(path)) {
            return path;
        }
        return null;
    }

    //
    //  File data: Image, Audio, Video, ...
    //

    public int saveFileData(byte[] data, String filename) {
        try {
            // save a copy to cache directory
            String path = LocalCache.getCacheFilePath(filename);
            return LocalCache.saveBinary(data, path);
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public String uploadEncryptedData(byte[] data, String filename, ID sender) {

        // prepare filename (make sure that filenames won't conflict)
        String ext = Paths.extension(filename);
        filename = Hex.encode(MD5.digest(data));
        if (ext != null && ext.length() > 0) {
            filename = filename + "." + ext;
        }

        // upload to CDN
        String upload = config.getUploadURL();
        assert upload != null : "upload API error";
        upload = upload.replaceAll("\\{ID\\}", sender.getAddress().toString());

        HTTPClient http = HTTPClient.getInstance();
        http.upload(upload, "file", filename, data);

        // build download URL
        String download = config.getDownloadURL();
        assert download != null : "download API error";
        return download.replaceAll("\\{ID\\}", sender.getAddress().toString()).replaceAll("\\{filename\\}", filename);
    }

    public String downloadEncryptedData(String url) {

        HTTPClient httpClient = HTTPClient.getInstance();
        return httpClient.download(url);
    }

    public String getFilePath(FileContent content) {
        String filePath;
        // check decrypted file
        String filename = content.getFilename();
        if (filename == null) {
            filePath = null;
        } else {
            filePath = LocalCache.getCacheFilePath(filename);
            if (Paths.exists(filePath)) {
                return filePath;
            }
        }
        // get encrypted data
        String url = content.getURL();
        if (url != null) {
            String cachePath = LocalCache.getCacheFilePath(url);
            try {
                byte[] data = decryptFile(cachePath, content.getPassword());
                if (data != null) {
                    if (LocalCache.saveBinary(data, filePath) == data.length) {
                        // success
                        return filePath;
                    }
                    // TODO: remove cachePath
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // download again
            downloadEncryptedData(url);
        }
        return null;
    }

    private byte[] decryptFile(String path, Map<String, Object> password) throws IOException {
        if (!Paths.exists(path)) {
            return null;
        }
        byte[] data = LocalCache.loadBinary(path);
        if (data == null) {
            // file not found
            return null;
        }
        SymmetricKey key = SymmetricKey.parse(password);
        if (key == null) {
            // key error
            return null;
        }
        return key.decrypt(data);
    }

    //
    //  HTTP Delegate
    //

    @Override
    public void uploadSuccess(UploadTask task, String response) {
        Log.info("upload respond: " + response);
        Map<String, Object> info = new HashMap<>();
        info.put("URL", task.getUrlString());
        info.put("filename", task.getFileName());
        info.put("name", task.getVarName());
        info.put("data", task.getFileData());
        info.put("response", response);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileUploadSuccess, this, info);
    }

    @Override
    public void uploadFailed(UploadTask task, IOException error) {
        Map<String, Object> info = new HashMap<>();
        info.put("URL", task.getUrlString());
        info.put("filename", task.getFileName());
        info.put("name", task.getVarName());
        info.put("data", task.getFileData());
        info.put("error", error);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileUploadFailure, this, info);
    }

    @Override
    public void downloadSuccess(DownloadTask task, String filePath) {
        Map<String, Object> info = new HashMap<>();
        info.put("URL", task.getUrlString());
        info.put("path", filePath);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileDownloadSuccess, this, info);
    }

    @Override
    public void downloadFailed(DownloadTask task, IOException error) {
        Map<String, Object> info = new HashMap<>();
        info.put("URL", task.getUrlString());
        info.put("error", error);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.FileDownloadFailure, this, info);
    }
}
