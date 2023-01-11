/* license: https://mit-license.org
 *
 *  File System
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
package chat.dim.filesys;

import chat.dim.http.HTTPClient;

/**
 *  RAM access
 */
public abstract class LocalCache extends ExternalStorage {

    public static String getRoot() {
        HTTPClient http = HTTPClient.getInstance();
        return http.getRoot();
    }

    /**
     *  Get cache file path: "/sdcard/chat.dim.sechat/caches/{XX}/{YY}/{filename}"
     *
     * @param filename - cache file name
     * @return cache file path
     */
    public static String getCacheFilePath(String filename) {
        assert filename.length() > 4 : "filename too short " + filename;
        String dir = getCachesDirectory();
        String xx = filename.substring(0, 2);
        String yy = filename.substring(2, 4);
        return appendPathComponent(dir, xx, yy, filename);
    }

    public static String getCachesDirectory() {
        return appendPathComponent(getRoot(), "caches");
    }

    /**
     *  Get temporary file path: "/sdcard/chat.dim.sechat/tmp/{filename}"
     *
     * @param filename - temporary file name
     * @return temporary file path
     */
    public static String getTemporaryFilePath(String filename) {
        String dir = getTemporaryDirectory();
        return appendPathComponent(dir, filename);
    }

    public static String getTemporaryDirectory() {
        return appendPathComponent(getRoot(), "tmp");
    }
}
