/* license: https://mit-license.org
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
package chat.dim.database;

import java.io.IOException;

import chat.dim.Address;
import chat.dim.ID;
import chat.dim.filesys.ExternalStorage;

public class Database extends ExternalStorage {

    public static String getParentDirectory(String path) {
        int pos = path.lastIndexOf(separator);
        if (pos < 0) {
            return null;
        } else if (pos == 0) {
            return separator;  // root dir: "/"
        }
        return path.substring(0, pos);
    }

    /**
     *  Get cache file path: "/sdcard/chat.dim.sechat/caches/{XX}/{filename}"
     *
     * @param filename - cache file name
     * @return cache file path
     */
    public static String getCacheFilePath(String filename) throws IOException {
        return getCacheDirectory(filename) + separator + filename;
    }

    public static String getCacheDirectory(String filename) throws IOException {
        assert filename.length() > 2 : "filename too short " + filename;
        String dir = getCachesDirectory() + separator + filename.substring(0, 2);
        return mkdirs(dir);
    }

    public static String getCachesDirectory() throws IOException {
        String dir = root + separator + "caches";
        return mkdirs(dir);
    }

    /**
     *  Get temporary file path: "/sdcard/chat.dim.sechat/tmp/{filename}"
     *
     * @param filename - temporary file name
     * @return temporary file path
     */
    public static String getTemporaryFilePath(String filename) throws IOException {
        return getTemporaryDirectory() + separator + filename;
    }

    public static String getTemporaryDirectory() throws IOException {
        String dir = root + separator + "tmp";
        return mkdirs(dir);
    }

    /**
     *  Get common file path: "/sdcard/chat.dim.sechat/dim/{filename}"
     *
     * @param filename - common file name
     * @return common file path
     */
    public static String getCommonFilePath(String filename) throws IOException {
        return getCommonDirectory() + separator + filename;
    }

    public static String getCommonDirectory() throws IOException {
        String dir = root + separator + "dim";
        return mkdirs(dir);
    }

    /**
     *  Get service provider's file path: "/sdcard/chat.dim.sechat/dim/{address}/{filename}"
     *
     * @param sp       - sp ID
     * @param filename - sp file name
     * @return sp file path
     */
    public static String getProviderFilePath(ID sp, String filename) throws IOException {
        return getProviderDirectory(sp.address) + separator + filename;
    }

    public static String getProviderDirectory(ID sp) throws IOException {
        return getProviderDirectory(sp.address);
    }

    public static String getProviderDirectory(Address address) throws IOException {
        String dir = getCommonDirectory() + separator + address.toString();
        return mkdirs(dir);
    }

    /**
     *  Get message file path: "/sdcard/chat.dim.sechat/dkd/{address}/messages.js"
     *
     * @param conversation - conversation ID
     * @return message file path
     */
    public static String getMessageFilePath(ID conversation) throws IOException {
        return getMessageDirectory(conversation.address) + separator + "messages.js";
    }

    public static String getMessageDirectory(ID conversation) throws IOException {
        return getMessageDirectory(conversation.address);
    }

    public static String getMessageDirectory(Address address) throws IOException {
        String dir = getMessageDirectory() + separator + address.toString();
        return mkdirs(dir);
    }

    public static String getMessageDirectory() throws IOException {
        String dir = root + separator + "dkd";
        return mkdirs(dir);
    }

    /**
     *  Get entity file path: "/sdcard/chat.dim.sechat/mkm/{XX}/{address}/{filename}"
     *
     * @param entity   - user or group ID
     * @param filename - entity file name
     * @return entity file path
     */
    public static String getEntityFilePath(ID entity, String filename) throws IOException {
        return getEntityDirectory(entity.address) + separator + filename;
    }

    public static String getEntityDirectory(ID entity) throws IOException {
        return getEntityDirectory(entity.address);
    }

    public static String getEntityDirectory(Address address) throws IOException {
        String string = address.toString();
        String dir = root + separator + "mkm" + separator
                + string.substring(0, 2) + separator
                + string;
        return mkdirs(dir);
    }

    /**
     *  Get user's private file path: "/sdcard/chat.dim.sechat/.private/{address}/{filename}"
     *
     * @param user     - user ID
     * @param filename - private file name
     * @return private file path
     */
    public static String getUserPrivateFilePath(ID user, String filename) throws IOException {
        return getUserPrivateFilePath(user.address, filename);
    }

    public static String getUserPrivateFilePath(Address address, String filename) throws IOException {
        return getUserPrivateDirectory(address) + separator + filename;
    }

    public static String getUserPrivateDirectory(ID user) throws IOException {
        return getUserPrivateDirectory(user.address);
    }

    public static String getUserPrivateDirectory(Address address) throws IOException {
        String dir = getPrivateDirectory() + separator + address.toString();
        return mkdirs(dir);
    }

    public static String getPrivateDirectory() throws IOException {
        String dir = root + separator + ".private";
        return mkdirs(dir);
    }

    public static String getProtectedDirectory() throws IOException {
        String dir = root + separator + ".protected";
        return mkdirs(dir);
    }

    public static String getProtectedFilePath(String filename) throws IOException {
        return getProtectedDirectory() + separator + filename;
    }
}
