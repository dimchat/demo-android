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
package chat.dim.common;

import java.io.IOException;
import java.util.Map;

import chat.dim.core.KeyCache;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;

public class KeyStore extends KeyCache {

    private static final KeyStore ourInstance = new KeyStore();
    public static KeyStore getInstance() { return ourInstance; }
    private KeyStore() {
        super();
    }

    // '/tmp/.dim/protected/keystore.js'
    private String getPath() {
        String root = ExternalStorage.root;
        return Paths.appendPathComponent(root, "protected", "keystore.js");
    }

    @Override
    public boolean saveKeys(Map keyMap) {
        try {
            String path = getPath();
            return ExternalStorage.saveJSON(keyMap, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map loadKeys() {
        try {
            String path = getPath();
            return (Map) ExternalStorage.loadJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
