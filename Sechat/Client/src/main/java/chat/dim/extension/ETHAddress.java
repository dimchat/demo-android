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
package chat.dim.extension;

import chat.dim.Address;
import chat.dim.NetworkType;

/**
 *  Address like Ethereum
 *
 *      data format: "0x0123456789ABCDEF"
 *
 *      algorithm:
 *          digest  = keccak256(pub_key_data)
 *          address = "0x" + hex_encode(digest)
 *
 *      checksum algorithm:
 *          str1 = hex_encode(digest)
 *          str2 = hex_encode(keccak256(str1))
 *          if str2[i] >= '8',
 *              str1[i] = uppercase(str1[i])
 *          address = "0x" + str1
 */
public final class ETHAddress extends Address {

    public ETHAddress(String string) {
        super(string);
        // TODO: decode ETH address
    }

    @Override
    public NetworkType getNetwork() {
        // ETH address always be personal;
        // If you want to create a group Address, use the default address format.
        return NetworkType.Main;
    }

    @Override
    public long getCode() {
        // TODO: use the last 4 bytes of address
        return 0;
    }

    /**
     *  Generate address with public key data
     *
     * @param key = key data
     * @return Address object
     */
    static ETHAddress generate(byte[] key) {
        assert key != null;
        // TODO: generate ETH address with public key data
        return null;
    }
}
