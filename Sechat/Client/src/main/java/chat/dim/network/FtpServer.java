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
import java.util.Map;

import chat.dim.ID;
import chat.dim.database.Database;
import chat.dim.digest.MD5;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Resources;
import chat.dim.format.Hex;
import chat.dim.http.HTTPClient;
import chat.dim.utils.StringUtils;

public class FtpServer {
    private static final FtpServer ourInstance = new FtpServer();
    public static FtpServer getInstance() { return ourInstance; }
    private FtpServer() {
        super();
    }

    private String apiUpload = null;
    private String apiDownload = null;
    private String apiAvatar = null;

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Map<String, String> info = null;
        try {
            info = (Map<String, String>) Resources.loadJSON("ftp.js");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (info == null) {
            return;
        }
        String upload = info.get("upload");
        if (upload != null) {
            apiUpload = upload;
        }
        String download = info.get("download");
        if (download != null) {
            apiDownload = download;
        }
        String avatar = info.get("avatar");
        if (avatar != null) {
            apiAvatar = avatar;
        }
    }

    public String getUserAgent() {
        String model = "Android";
        String sysName = "HMS";
        String sysVersion = "4.0";
        String lang = "zh-CN";

        return String.format("DIMP/1.0 (%s; U; %s %s; %s)" +
                        " DIMCoreKit/1.0 (Terminal, like WeChat) DIM-by-GSP/1.0.1",
                model, sysName, sysVersion, lang);
    }

    // "https://sechat.dim.chat/{ID}}/upload"
    public String getUploadAPI() {
        if (apiUpload == null) {
            loadConfig();
        }
        return apiUpload;
    }

    // "https://sechat.dim.chat/download/{ID}/{filename}"
    public String getDownloadAPI() {
        if (apiDownload == null) {
            loadConfig();
        }
        return apiDownload;
    }

    // "https://sechat.dim.chat/avatar/{ID}/{filename}"
    public String getAvatarAPI() {
        if (apiAvatar == null) {
            loadConfig();
        }
        return apiAvatar;
    }

    //
    //  Avatar
    //

    public String uploadAvatar(byte[] imageData, ID identifier) {

        String filename = Hex.encode(MD5.digest(imageData)) + ".jpeg";

        // upload to CDN
        String upload = getUploadAPI();
        assert upload != null : "upload API error";
        upload = upload.replaceAll("\\{ID\\}", identifier.toString());

        HTTPClient httpClient = HTTPClient.getInstance();
        httpClient.upload(imageData, upload, filename, "avatar");

        // build download URL
        String download = getAvatarAPI();
        assert download != null : "download API error";
        download = download.replaceAll("\\{ID\\}", identifier.toString()).replaceAll("\\{filename\\}", filename);

        // store in user's directory
        String path = Database.getEntityFilePath(identifier, "avatar.jpeg");
        try {
            ExternalStorage.saveData(imageData, path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // save a copy to cache directory
        saveImage(imageData, filename);

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

        path = Database.getEntityFilePath(identifier, "avatar.jpeg");
        if (ExternalStorage.exists(path)) {
            return path;
        }
        return null;
    }

    //
    //  File data: Image, Audio, Video, ...
    //

    public boolean saveImage(byte[] imageData, String filename) {
        // save a copy to cache directory
        String path = Database.getCacheFilePath(filename);
        try {
            return ExternalStorage.saveData(imageData, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String uploadEncryptedData(byte[] data, String filename, ID sender) {

        // prepare filename (make sure that filenames won't conflict)
        filename = Hex.encode(MD5.digest(data));
        String ext = StringUtils.extension(filename);
        if (ext != null && ext.length() > 0) {
            filename = filename + "." + ext;
        }

        // upload to CDN
        String upload = getUploadAPI();
        assert upload != null : "upload API error";
        upload = upload.replaceAll("\\{ID\\}", sender.toString());

        HTTPClient httpClient = HTTPClient.getInstance();
        httpClient.upload(data, upload, filename, "file");

        // build download URL
        String download = getAvatarAPI();
        assert download != null : "download API error";
        return download.replaceAll("\\{ID\\}", sender.toString()).replaceAll("\\{filename\\}", filename);
    }

    public String downloadEncryptedData(String url) {

        HTTPClient httpClient = HTTPClient.getInstance();
        return httpClient.download(url);
    }
}
