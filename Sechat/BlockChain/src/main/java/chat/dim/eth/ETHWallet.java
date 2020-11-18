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
package chat.dim.eth;

import java.util.HashMap;
import java.util.Map;

import chat.dim.blockchain.Wallet;
import chat.dim.threading.BackgroundThreads;

public class ETHWallet implements Wallet {

    private final String address;

    public ETHWallet(String address) {
        super();
        this.address = address;
    }

    @Override
    public long getBalance() {
        Ethereum client = Ethereum.getInstance();
        return client.getBalance(address);
    }

    private static final Map<String, Double> balances = new HashMap<>();

    public static double getBalance(String address) {
        BackgroundThreads.wait(() -> {
            ETHWallet wallet = new ETHWallet(address);
            double eth = wallet.getBalance() / 1000000000000000000.0;
            balances.put(address, eth);
        });
        Object eth = balances.get(address);
        if (eth == null) {
            return  -1;
        }
        return (double) eth;
    }
}
