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
import chat.dim.crypto.SymmetricKey;
import chat.dim.database.Database;
import chat.dim.digest.MD5;
import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Hex;
import chat.dim.http.HTTPClient;
import chat.dim.model.Configuration;
import chat.dim.protocol.FileContent;
import chat.dim.utils.Strings;

public class FtpServer {
    private static final FtpServer ourInstance = new FtpServer();
    public static FtpServer getInstance() { return ourInstance; }
    private FtpServer() {
        super();
    }

    private Configuration config = Configuration.getInstance();

    //
    //  Avatar
    //

    public String uploadAvatar(byte[] imageData, ID identifier) {

        String filename = Hex.encode(MD5.digest(imageData)) + ".jpeg";

        // upload to CDN
        String upload = config.getUploadURL();
        assert upload != null : "upload API error";
        upload = upload.replaceAll("\\{ID\\}", identifier.address.toString());

        HTTPClient httpClient = HTTPClient.getInstance();
        httpClient.upload(imageData, upload, filename, "avatar");

        // build download URL
        String download = config.getAvatarURL();
        assert download != null : "download API error";
        download = download.replaceAll("\\{ID\\}", identifier.address.toString()).replaceAll("\\{filename\\}", filename);

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
        String ext = Strings.extension(filename);
        filename = Hex.encode(MD5.digest(data));
        if (ext != null && ext.length() > 0) {
            filename = filename + "." + ext;
        }

        // upload to CDN
        String upload = config.getUploadURL();
        assert upload != null : "upload API error";
        upload = upload.replaceAll("\\{ID\\}", sender.address.toString());

        HTTPClient httpClient = HTTPClient.getInstance();
        httpClient.upload(data, upload, filename, "file");

        // build download URL
        String download = config.getDownloadURL();
        assert download != null : "download API error";
        return download.replaceAll("\\{ID\\}", sender.address.toString()).replaceAll("\\{filename\\}", filename);
    }

    public String downloadEncryptedData(String url) {

        HTTPClient httpClient = HTTPClient.getInstance();
        return httpClient.download(url);
    }

    public String getFilePath(FileContent content) {
        // check decrypted file
        String filename = content.getFilename();
        String path1 = Database.getCacheFilePath(filename);
        if (ExternalStorage.exists(path1)) {
            return path1;
        }
        // get encrypted data
        String url = content.getURL();
        String path2 = HTTPClient.getCachePath(url);
        byte[] data = null;
        try {
            data = decryptFile(path2, content.getPassword());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (data != null) {
            try {
                if (ExternalStorage.saveData(data, path1)) {
                    return path1;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // download again
        downloadEncryptedData(url);
        return null;
    }

    private byte[] decryptFile(String path, Map<String, Object> password) throws IOException, ClassNotFoundException {
        if (!ExternalStorage.exists(path)) {
            return null;
        }
        byte[] data = ExternalStorage.loadData(path);
        if (data == null) {
            // file not found
            return null;
        }
        SymmetricKey key = SymmetricKey.getInstance(password);
        if (key == null) {
            // key error
            return null;
        }
        return key.decrypt(data);
    }
}
