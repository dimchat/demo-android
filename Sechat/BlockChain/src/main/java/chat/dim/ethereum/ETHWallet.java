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
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import chat.dim.notification.NotificationCenter;
import chat.dim.protocol.Address;
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
    public ETHWallet(Address address) {
        super();
        this.credentials = null;
        this.address = address.toString();
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
                String event;
                Map<String, Object> info = new HashMap<>();
                info.put("name", WalletName.ETH.getValue());
                info.put("address", address);
                // get ETH balance
                Ethereum client = Ethereum.getInstance();
                EthGetBalance ethGetBalance = client.ethGetBalance(address);
                if (ethGetBalance == null || ethGetBalance.hasError()) {
                    event = Wallet.BalanceQueryFailed;
                } else {
                    event = Wallet.BalanceUpdated;
                    BigDecimal balance = getBalance(ethGetBalance);
                    setBalance(balance);
                    info.put("balance", balance.doubleValue());
                }
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(event, this, info);
            });
        }
        return getBalance();
    }

    @Override
    public boolean transfer(Address toAddress, double coins) {
        double balance = getBalance();
        if (balance < coins) {
            // balance not enough
            return false;
        }
        BackgroundThreads.rush(() -> {
            String event;
            Map<String, Object> info = new HashMap<>();
            info.put("name", WalletName.ETH.getValue());
            info.put("address", address);
            info.put("to", toAddress.toString());
            info.put("amount", coins);
            // send funds
            Ethereum client = Ethereum.getInstance();
            if (gasPrice == null || gasLimit == null) {
                TransactionReceipt receipt = client.sendFunds(credentials, toAddress.toString(), new BigDecimal(coins));
                if (receipt == null) {
                    event = Wallet.TransactionError;
                    info.put("balance", balance);
                } else {
                    event = Wallet.TransactionSuccess;
                    info.put("balance", balance - coins);
                    info.put("transactionHash", receipt.getTransactionHash());
                }
            } else {
                EthSendTransaction tx = client.ethSendTransaction(credentials, toAddress.toString(), toWei(coins), gasPrice, gasLimit);
                if (tx == null || tx.hasError()) {
                    event = Wallet.TransactionError;
                    info.put("balance", balance);
                } else {
                    event = Wallet.TransactionSuccess;
                    info.put("balance", balance - coins);
                    info.put("transactionHash", tx.getTransactionHash());
                }
            }
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(event, this, info);
        });
        return true;
    }

    //
    //  Memory caches for Transaction
    //
    static private final Map<String, Transaction> transactions = new HashMap<>();
    static private final Map<String, TransactionReceipt> receipts = new HashMap<>();

    private Transaction getTransaction(String txHash) {
        return transactions.get(txHash);
    }
    private void setTransaction(Transaction tx) {
        transactions.put(tx.getHash(), tx);
    }
    private TransactionReceipt getReceipt(String txHash) {
        return receipts.get(txHash);
    }
    private void setReceipt(TransactionReceipt receipt) {
        receipts.put(receipt.getTransactionHash(), receipt);
    }

    public Transaction getTransactionByHash(String txHash) {
        Transaction tx = getTransaction(txHash);
        if (tx == null) {
            BackgroundThreads.rush(() -> {
                String event;
                Map<String, Object> info = new HashMap<>();
                info.put("transactionHash", txHash);
                // send funds
                Ethereum client = Ethereum.getInstance();
                EthTransaction ethTransaction = client.ethGetTransactionByHash(txHash);
                if (ethTransaction == null || ethTransaction.hasError()) {
                    event = Wallet.TransactionError;
                } else {
                    Transaction result = ethTransaction.getResult();
                    if (isTransactionAccepted(result)) {
                        event = Wallet.TransactionSuccess;
                        setTransaction(result);
                        info.put("transactionHash", result.getHash());
                    } else {
                        event = Wallet.TransactionWaiting;
                    }
                }
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(event, this, info);
            });
        }
        return tx;
    }

    private boolean isTransactionAccepted(Transaction tx) {
        String blockHash = tx.getBlockHash();
        return Numeric.toBigInt(blockHash).compareTo(BigInteger.ZERO) != 0;
    }

    public TransactionReceipt getTransactionReceipt(String txHash) {
        TransactionReceipt receipt = getReceipt(txHash);
        if (receipt == null) {
            BackgroundThreads.rush(() -> {
                String event;
                Map<String, Object> info = new HashMap<>();
                info.put("transactionHash", txHash);
                // send funds
                Ethereum client = Ethereum.getInstance();
                EthGetTransactionReceipt getReceipt = client.ethGetTransactionReceipt(txHash);
                if (getReceipt == null || getReceipt.hasError()) {
                    event = Wallet.TransactionError;
                } else {
                    TransactionReceipt result = getReceipt.getResult();
                    if (result.isStatusOK()) {
                        event = Wallet.TransactionSuccess;
                        setReceipt(result);
                        info.put("transactionHash", result.getTransactionHash());
                    } else {
                        event = Wallet.TransactionWaiting;
                    }
                }
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(event, this, info);
            });
        }
        return receipt;
    }
}
