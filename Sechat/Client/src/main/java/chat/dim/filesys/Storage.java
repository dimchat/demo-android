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
package chat.dim.filesys;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Storage implements Writable {

    private byte[] fileContent = null;

    //---- read

    @Override
    public boolean exists(String filename) {
        File file = new File(filename);
        return file.exists() && file.length() > 0;
    }

    @Override
    public int load(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            // file not found
            throw new IOException("file not found: " + filename);
        }
        InputStream is = new FileInputStream(file);
        int size = is.available();
        fileContent = new byte[size];
        int len = is.read(fileContent, 0, size);
        is.close();
        if (len != size) {
            throw new IOException("reading error(" + len + " != " + size + "): " + filename);
        }
        return len;
    }

    @Override
    public byte[] getData() {
        return fileContent;
    }

    //---- write

    @Override
    public void setData(byte[] data) {
        fileContent = data;
    }

    @Override
    public int save(String filename) throws IOException {
        if (fileContent == null) {
            return -1;
        }
        File file = new File(filename);
        if (!file.exists()) {
            // check parent directory exists
            File dir = file.getParentFile();
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IOException("failed to create directory: " + dir);
            }
        }
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(fileContent);
        fos.close();
        return fileContent.length;
    }

    @Override
    public boolean remove(String filename) throws IOException {
        File file = new File(filename);
        if (!file.exists()) {
            throw new IOException("file not found: " + filename);
        }
        return file.delete();
    }
}
