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
package chat.dim.ethereum;

import java.util.HashMap;
import java.util.Map;

import chat.dim.notification.NotificationCenter;
import chat.dim.threading.BackgroundThreads;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public abstract class ERC20Wallet implements Wallet {

    private final String contactAddress;
    private final String address;
    private double balance = 0;

    public ERC20Wallet(String address, String contactAddress) {
        super();
        this.address = address;
        this.contactAddress = contactAddress;
    }

    abstract protected WalletName getName();

    abstract protected double getBalance(String erc20Balance);

    @Override
    public double getBalance(boolean refresh) {
        if (!refresh) {
            return balance;
        }
        BackgroundThreads.rush(() -> {
            Ethereum client = Ethereum.getInstance();
            String result = client.erc20GetBalance(address, contactAddress);
            if (result == null) {
                // TODO: failed to get ERC20 balance
                return;
            }
            balance = getBalance(result);
            // post notification
            Map<String, Object> info = new HashMap<>();
            info.put("name", getName().toString());
            info.put("address", address);
            info.put("balance", balance);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(Wallet.BalanceUpdated, this, info);
        });
        return balance;
    }

    @Override
    public boolean transfer(String toAddress, double amount) {
        return false;
    }
}
