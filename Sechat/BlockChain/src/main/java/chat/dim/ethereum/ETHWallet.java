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

import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import chat.dim.notification.NotificationCenter;
import chat.dim.threading.BackgroundThreads;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public class ETHWallet implements Wallet {

    private final String address;
    private BigDecimal balance = null;  // Unit: ETHER

    public ETHWallet(String address) {
        super();
        this.address = address;
    }

    /**
     *  Convert ETH balance from Wei to Ether
     *
     * @param ethBalance - wei
     * @return ether
     */
    private BigDecimal getBalance(BigInteger ethBalance) {
        BigDecimal wei = new BigDecimal(ethBalance);
        return Convert.fromWei(wei, Convert.Unit.ETHER);
    }

    @Override
    public double getBalance(boolean refresh) {
        if (refresh) {
            BackgroundThreads.rush(() -> {
                Ethereum client = Ethereum.getInstance();
                BigInteger ethBalance = client.ethGetBalance(address);
                if (ethBalance == null) {
                    // TODO: failed to get ETH balance
                    return;
                }
                balance = getBalance(ethBalance);
                // post notification
                Map<String, Object> info = new HashMap<>();
                info.put("name", WalletName.ETH.getValue());
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
        BackgroundThreads.rush(() -> {
            NotificationCenter nc = NotificationCenter.getInstance();
            Map<String, Object> info = new HashMap<>();
            info.put("name", WalletName.ETH.getValue());
            info.put("address", address);
            info.put("to", toAddress);
            info.put("amount", amount);

            Ethereum client = Ethereum.getInstance();
            TransactionReceipt receipt = client.sendFunds(address, toAddress, amount);
            if (receipt == null) {
                info.put("balance", balance.doubleValue());
                nc.postNotification(Wallet.TransactionError, this, info);
            } else {
                // TODO: process receipt
                balance = balance.subtract(new BigDecimal(amount));
                info.put("balance", balance.doubleValue());
                nc.postNotification(Wallet.TransactionSuccess, this, info);
            }
        });
        return true;
    }
}
