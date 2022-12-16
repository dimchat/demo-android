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

import chat.dim.protocol.Address;
import chat.dim.protocol.ID;

/**
 *  RAM access
 */
public abstract class EntityStorage extends ExternalStorage {

    /**
     *  Get entity file path: "/sdcard/chat.dim.sechat/mkm/{XX}/{YY}/{address}/{filename}"
     *
     * @param entity   - user or group ID
     * @param filename - entity file name
     * @return entity file path
     */
    public static String getEntityFilePath(ID entity, String filename) {
        String dir = getEntityDirectory(entity.getAddress());
        return appendPathComponent(dir, filename);
    }

    public static String getEntityDirectory(ID identifier) {
        return getEntityDirectory(identifier.getAddress());
    }

    public static String getEntityDirectory(Address address) {
        String string = address.toString();
        String dir = getEntityDirectory();
        String xx = string.substring(0, 2);
        String yy = string.substring(2, 4);
        return appendPathComponent(dir, xx, yy, string);
    }

    public static String getEntityDirectory() {
        return appendPathComponent(getRoot(), "mkm");
    }
}
