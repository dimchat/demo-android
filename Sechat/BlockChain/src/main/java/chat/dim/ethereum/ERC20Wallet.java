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

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import chat.dim.notification.NotificationCenter;
import chat.dim.threading.BackgroundThreads;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public abstract class ERC20Wallet implements Wallet {

    private final String contractAddress;
    private final String address;
    private BigDecimal balance = null;  // Unit: ERC20 contract

    ERC20Wallet(String address, String contractAddress) {
        super();
        this.address = address;
        this.contractAddress = contractAddress;
    }

    abstract protected WalletName getName();

    /**
     *  Convert ERC20 balance
     *
     * @param erc20Balance - smallest unit
     * @return normal unit
     */
    abstract protected BigDecimal getBalance(String erc20Balance);

    @Override
    public double getBalance(boolean refresh) {
        if (refresh) {
            BackgroundThreads.rush(() -> {
                Ethereum client = Ethereum.getInstance();
                String result = client.erc20GetBalance(address, contractAddress);
                if (result == null) {
                    // TODO: failed to get ERC20 balance
                    return;
                }
                balance = getBalance(result);
                // post notification
                Map<String, Object> info = new HashMap<>();
                info.put("name", getName().toString());
                info.put("address", address);
                info.put("balance", balance.doubleValue());
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(Wallet.BalanceUpdated, this, info);
            });
        }
        return balance == null ? 0 : balance.doubleValue();
    }

    @Override
    public boolean transfer(String toAddress, double amount) {
        if (balance == null || balance.doubleValue() < amount) {
            // balance not enough
            return false;
        }
        // TODO:
        return false;
    }
}
