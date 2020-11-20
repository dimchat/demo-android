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
package chat.dim.blockchain;

import java.util.HashMap;
import java.util.Map;

import chat.dim.eth.ETHWallet;

public class WalletFactory {

    static private final Map<Wallet.Name, Map<String, Wallet>> allWallets = new HashMap<>();

    public static Wallet getWallet(Wallet.Name name, String address) {
        Map<String, Wallet> caches = allWallets.get(name);
        if (caches == null) {
            caches = new HashMap<>();
            allWallets.put(name, caches);
        }
        Wallet wallet = caches.get(address);
        if (wallet == null) {
            wallet = creator.create(name, address);
            if (wallet != null) {
                caches.put(address, wallet);
            }
        }
        return wallet;
    }

    /**
     *  Wallet Creator
     */
    public interface Creator {
        Wallet create(Wallet.Name name, String address);
    }

    // default creator
    public static Creator creator = (name, address) -> {
        if (name.equals(WalletName.ETH)) {
            return new ETHWallet(address);
        }
        return null;
    };
}
