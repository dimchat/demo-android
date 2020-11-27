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

import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
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

    private final Credentials credentials;
    private final String address;

    private BigInteger gasPrice = null; //new BigInteger("58000000000");  // wei
    private BigInteger gasLimit = null; //new BigInteger("21000");

    public ETHWallet(Credentials credentials) {
        super();
        this.credentials = credentials;
        this.address = credentials.getAddress();
    }
    public ETHWallet(String address) {
        super();
        this.credentials = null;
        this.address = address;
    }

    public void setGasPrice(double gwei) {
        BigDecimal number = BigDecimal.valueOf(gwei);
        BigDecimal wei = Convert.toWei(number, Convert.Unit.GWEI);
        gasPrice = wei.toBigInteger();
    }
    public void setGasLimit(long gas) {
        gasLimit = BigInteger.valueOf(gas);
    }

    //
    //  Memory caches for Balances
    //
    static private final Map<String, BigDecimal> balances = new HashMap<>();  // Unit: ETHER

    private double getBalance() {
        BigDecimal balance = balances.get(address);
        if (balance == null) {
            return -1;
        }
        return balance.doubleValue();
    }
    private void setBalance(double balance) {
        setBalance(new BigDecimal(balance));
    }
    private void setBalance(BigDecimal balance) {
        balances.put(address, balance);
    }

    /**
     *  Convert ETH balance from Wei to Ether
     *
     * @param ethBalance - wei
     * @return ether
     */
    private BigDecimal getBalance(EthGetBalance ethBalance) {
        BigDecimal wei = new BigDecimal(ethBalance.getBalance());
        return Convert.fromWei(wei, Convert.Unit.ETHER);
    }

    /**
     *  Convert ETH balance from Ether to Wei
     *
     * @param coins - ether
     * @return wei
     */
    private BigInteger toWei(double coins) {
        BigDecimal wei = Convert.toWei(new BigDecimal(coins), Convert.Unit.ETHER);
        return wei.toBigInteger();
    }

    @Override
    public double getBalance(boolean refresh) {
        if (refresh) {
            BackgroundThreads.rush(() -> {
                NotificationCenter nc = NotificationCenter.getInstance();
                Map<String, Object> info = new HashMap<>();
                info.put("name", WalletName.ETH.getValue());
                info.put("address", address);
                // get ETH balance
                Ethereum client = Ethereum.getInstance();
                EthGetBalance ethGetBalance = client.ethGetBalance(address);
                if (ethGetBalance == null || ethGetBalance.hasError()) {
                    nc.postNotification(Wallet.BalanceQueryFailed, this, info);
                } else {
                    BigDecimal balance = getBalance(ethGetBalance);
                    setBalance(balance);
                    info.put("balance", balance.doubleValue());
                    nc.postNotification(Wallet.BalanceUpdated, this, info);
                }
            });
        }
        return getBalance();
    }

    private static String transfer(Credentials fromAccount, String toAddress, double eth) {
        Ethereum client = Ethereum.getInstance();
        TransactionReceipt receipt = client.sendFunds(fromAccount, toAddress, new BigDecimal(eth));
        if (receipt == null) {
            return null;
        } else {
            return receipt.getTransactionHash();
        }
    }
    private static String transfer(Credentials fromAccount, String toAddress, BigInteger sum,
                                   BigInteger gasPrice, BigInteger gasLimit) {
        Ethereum client = Ethereum.getInstance();
        EthSendTransaction tx = client.ethSendTransaction(fromAccount, toAddress, sum, gasPrice, gasLimit);
        if (tx == null || tx.hasError()) {
            return null;
        } else {
            return tx.getTransactionHash();
        }
    }

    @Override
    public boolean transfer(String toAddress, double coins) {
        double balance = getBalance();
        if (balance < coins) {
            // balance not enough
            return false;
        }
        BackgroundThreads.rush(() -> {
            NotificationCenter nc = NotificationCenter.getInstance();
            Map<String, Object> info = new HashMap<>();
            info.put("name", WalletName.ETH.getValue());
            info.put("address", address);
            info.put("to", toAddress);
            info.put("amount", coins);
            // send funds
            String txHash;
            if (gasPrice == null || gasLimit == null) {
                txHash = transfer(credentials, toAddress, coins);
            } else {
                txHash = transfer(credentials, toAddress, toWei(coins), gasPrice, gasLimit);
            }
            if (txHash == null) {
                info.put("balance", balance);
                nc.postNotification(Wallet.TransactionError, this, info);
            } else {
                // TODO: process receipt
                double remaining = balance - coins;
                setBalance(remaining);
                info.put("balance", remaining);
                info.put("transactionHash", txHash);
                nc.postNotification(Wallet.TransactionSuccess, this, info);
            }
        });
        return true;
    }
}
