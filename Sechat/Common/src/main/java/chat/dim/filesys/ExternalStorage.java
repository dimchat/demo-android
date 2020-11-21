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
package chat.dim.filesys;

import java.io.File;
import java.io.IOException;

import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;

public class ExternalStorage {

    // "/sdcard/chat.dim.sechat"
    private static String root = "/tmp/.dim";
    private static boolean made = false;

    protected static String separator = File.separator;

    public static String getRoot() {
        if (made) {
            return root;
        }
        try {
            mkdirs(root);
            // forbid the gallery from scanning media files
            String path = root + separator + ".nomedia";
            if (!exists(path)) {
                saveText("Moky loves May Lee forever!", path);
            }
            made = true;
            return root;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    public static void setRoot(String dir) {
        if (dir.length() > separator.length() && dir.endsWith(separator)) {
            root = dir.substring(0, dir.length() - separator.length());
        } else {
            root = dir;
        }
    }

    public static String mkdirs(String path) throws IOException {
        File dir = new File(path);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("failed to create directory: " + path);
        }
        return path;
    }

    //-------- read

    /**
     *  Check whether data file exists
     *
     * @param pathname - full path
     * @return True on exists
     */
    public static boolean exists(String pathname) {
        Storage file = new Storage();
        return file.exists(pathname);
    }

    /**
     *  Load data from file path
     *
     * @param pathname - full path
     * @return file data
     */
    public static byte[] loadData(String pathname) throws IOException {
        Storage file = new Storage();
        file.read(pathname);
        return file.getData();
    }

    /**
     *  Load text from file path
     *
     * @param pathname - full path
     * @return text string
     */
    public static String loadText(String pathname) throws IOException {
        byte[] data = loadData(pathname);
        if (data == null) {
            return null;
        }
        return UTF8.decode(data);
    }

    /**
     *  Load JSON from file path
     *
     * @param pathname - full path
     * @return Map/List object
     */
    public static Object loadJSON(String pathname) throws IOException {
        byte[] data = loadData(pathname);
        if (data == null) {
            return null;
        }
        return JSON.decode(data);
    }

    //-------- write

    /**
     *  Save data into binary file
     *
     * @param data - binary data
     * @param pathname - full path
     * @return true on success
     */
    public static boolean saveData(byte[] data, String pathname) throws IOException {
        Storage file = new Storage();
        file.setData(data);
        int len = file.write(pathname);
        return len == data.length;
    }

    /**
     *  Save string into Text file
     *
     * @param text - text string
     * @param pathname - full path
     * @return true on success
     */
    public static boolean saveText(String text, String pathname) throws IOException {
        byte[] data = UTF8.encode(text);
        return saveData(data, pathname);
    }

    /**
     *  Save Map/List into JSON file
     *
     * @param object - Map/List object
     * @param pathname - full path
     * @return true on success
     */
    public static boolean saveJSON(Object object, String pathname) throws IOException {
        byte[] json = JSON.encode(object);
        return saveData(json, pathname);
    }

    /**
     *  Delete file
     *
     * @param pathname - full path
     * @return true on success
     */
    public static boolean delete(String pathname) throws IOException {
        Storage file = new Storage();
        return file.remove(pathname);
    }

    //
    //  Directories
    //

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
     *  Get cache file path: "/sdcard/chat.dim.sechat/caches/{XX}/{YY}/{filename}"
     *
     * @param filename - cache file name
     * @return cache file path
     */
    public static String getCacheFilePath(String filename) throws IOException {
        return getCacheDirectory(filename) + separator + filename;
    }

    public static String getCacheDirectory(String filename) throws IOException {
        assert filename.length() > 4 : "filename too short " + filename;
        String dir = getCachesDirectory() + separator
                + filename.substring(0, 2) + separator
                + filename.substring(2, 4);
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
     *  Get entity file path: "/sdcard/chat.dim.sechat/mkm/{XX}/{YY}/{address}/{filename}"
     *
     * @param entity   - user or group ID
     * @param filename - entity file name
     * @return entity file path
     */
    public static String getEntityFilePath(ID entity, String filename) throws IOException {
        return getEntityDirectory(entity.getAddress()) + separator + filename;
    }

    public static String getEntityDirectory(Address address) throws IOException {
        String string = address.toString();
        String dir = root + separator + "mkm" + separator
                + string.substring(0, 2) + separator
                + string.substring(2, 4) + separator
                + string;
        return mkdirs(dir);
    }
}
