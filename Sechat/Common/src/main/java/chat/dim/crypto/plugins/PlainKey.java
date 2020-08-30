/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
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
package chat.dim.crypto.plugins;

import java.util.HashMap;
import java.util.Map;

import chat.dim.crypto.SymmetricKey;

/**
 *  Symmetric key for broadcast message,
 *  which will do nothing when en/decoding message data
 */
public final class PlainKey extends SymmetricKey {

    private final static String PLAIN = "PLAIN";

    public PlainKey(Map<String, Object> dictionary) {
        super(dictionary);
    }

    @Override
    public byte[] encrypt(byte[] plaintext) {
        return plaintext;
    }

    @Override
    public byte[] decrypt(byte[] ciphertext) {
        return ciphertext;
    }

    @Override
    public byte[] getData() {
        return new byte[0];
    }

    //-------- Runtime --------

    private static SymmetricKey ourInstance = null;

    public static SymmetricKey getInstance() {
        if (ourInstance == null) {
            Map<String, Object> dictionary = new HashMap<>();
            dictionary.put("algorithm", PLAIN);
            ourInstance = new PlainKey(dictionary);
        }
        return ourInstance;
    }

    static {
        // PLAIN
        register(PLAIN, PlainKey.class);
    }
}
