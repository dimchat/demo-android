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
import java.nio.charset.Charset;

import chat.dim.filesys.Storage;
import chat.dim.format.JSON;
import chat.dim.utils.Log;

public class ExternalStorage {

    public static String root = "/tmp/.dim";

    //-------- read

    public static boolean exists(String filename) throws IOException {
        Storage file = new Storage();
        return file.exists(filename);
    }

    public static byte[] read(String filename) throws IOException {
        Storage file = new Storage();
        file.load(filename);
        return file.getData();
    }

    public static String readText(String filename) throws IOException {
        byte[] data = read(filename);
        if (data == null) {
            return null;
        }
        return new String(data, Charset.forName("UTF-8"));
    }

    public static Object readJSON(String filename) throws IOException {
        String string = readText(filename);
        if (string == null) {
            return null;
        }
        return JSON.decode(string);
    }

    //-------- write

    public static boolean write(byte[] data, String filename) throws IOException {
        Storage file = new Storage();
        file.setData(data);
        int len = file.save(filename);
        Log.info("wrote " + len + " byte(s) into " + filename);
        return len == data.length;
    }

    public static boolean writeText(String text, String filename) throws IOException {
        byte[] data = text.getBytes(Charset.forName("UTF-8"));
        return write(data, filename);
    }

    public static boolean writeJSON(Object object, String filename) throws IOException {
        String json = JSON.encode(object);
        return writeText(json, filename);
    }

    public static boolean delete(String filename) throws IOException {
        Storage file = new Storage();
        return file.remove(filename);
    }
}
