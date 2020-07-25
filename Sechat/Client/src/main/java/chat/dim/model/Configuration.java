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
package chat.dim.model;

import java.io.IOException;
import java.util.Map;

import chat.dim.filesys.Resources;

public class Configuration {

    private static final Configuration ourInstance = new Configuration();
    public static Configuration getInstance() { return ourInstance; }
    private Configuration() {
        super();
        loadConfig();
    }

    private String apiUpload = null;
    private String apiDownload = null;
    private String apiAvatar = null;

    private String apiTerms = null;
    private String apiAbout = null;

    @SuppressWarnings("unchecked")
    private void loadConfig() {
        Map<String, String> apis = null;
        try {
            Map<String, Object> info = (Map<String, Object>) Resources.loadJSON("gsp.js");
            if (info != null) {
                apis = (Map<String, String>) info.get("APIs");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (apis == null) {
            return;
        }

        String upload = apis.get("upload");
        if (upload != null) {
            apiUpload = upload;
        }
        String download = apis.get("download");
        if (download != null) {
            apiDownload = download;
        }
        String avatar = apis.get("avatar");
        if (avatar != null) {
            apiAvatar = avatar;
        }

        String terms = apis.get("terms");
        if (terms != null) {
            apiTerms = terms;
        }
        String about = apis.get("about");
        if (about != null) {
            apiAbout = about;
        }
    }

    // "https://sechat.dim.chat/{ID}}/upload"
    public String getUploadURL() {
        if (apiUpload == null) {
            loadConfig();
        }
        return apiUpload;
    }

    // "https://sechat.dim.chat/download/{ID}/{filename}"
    public String getDownloadURL() {
        if (apiDownload == null) {
            loadConfig();
        }
        return apiDownload;
    }

    // "https://sechat.dim.chat/avatar/{ID}/{filename}"
    public String getAvatarURL() {
        if (apiAvatar == null) {
            loadConfig();
        }
        return apiAvatar;
    }

    // "https://wallet.dim.chat/dimchat/sechat/privacy.html"
    public String getTermsURL() {
        if (apiTerms == null) {
            loadConfig();
        }
        return apiTerms;
    }

    // "https://dim.chat/sechat"
    // "https://sechat.dim.chat/support"
    public String getAboutURL() {
        if (apiAbout == null) {
            loadConfig();
        }
        return apiAbout;
    }
}