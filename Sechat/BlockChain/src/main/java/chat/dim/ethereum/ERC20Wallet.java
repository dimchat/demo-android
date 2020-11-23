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

import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import chat.dim.notification.NotificationCenter;
import chat.dim.threading.BackgroundThreads;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public abstract class ERC20Wallet implements Wallet {

    private final Credentials credentials;
    private final String address;
    private final String contractAddress;

    public BigInteger gasPrice = new BigInteger("58000000000");  // wei
    public BigInteger gasLimit = new BigInteger("21000");

    ERC20Wallet(Credentials credentials, String contractAddress) {
        super();
        this.credentials = credentials;
        this.address = credentials.getAddress();
        this.contractAddress = contractAddress;
    }
    ERC20Wallet(String address, String contractAddress) {
        super();
        this.credentials = null;
        this.address = address;
        this.contractAddress = contractAddress;
    }

    //
    //  Memory caches for Balances
    //
    static private final Map<WalletName, Map<String, BigDecimal>> allBalances = new HashMap<>();  // Unit: ERC20 contract

    private double getBalance() {
        Map<String, BigDecimal> balances = allBalances.get(getName());
        if (balances == null) {
            return 0;
        }
        BigDecimal balance = balances.get(address);
        if (balance == null) {
            return 0;
        }
        return balance.doubleValue();
    }
    private void setBalance(double balance) {
        setBalance(new BigDecimal(balance));
    }
    private void setBalance(BigDecimal balance) {
        Map<String, BigDecimal> balances = allBalances.get(getName());
        if (balances == null) {
            balances = new HashMap<>();
            allBalances.put(getName(), balances);
        }
        balances.put(address, balance);
    }

    abstract protected WalletName getName();

    /**
     *  Convert ERC20 balance to coins
     *
     * @param erc20Balance - smallest unit
     * @return normal unit
     */
    abstract protected BigDecimal getBalance(EthCall erc20Balance);

    /**
     *  Convert ERC20 coins to balance
     *
     * @param coins - normal unit
     * @return smallest unit
     */
    abstract protected BigInteger toBalance(double coins);

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private EthCall queryBalance() {
        Function function = new Function("balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Address>() {}));
        String encode = FunctionEncoder.encode(function);
        Transaction tx = Transaction.createEthCallTransaction(address, contractAddress, encode);
        Ethereum client = Ethereum.getInstance();
        return client.ethCall(tx);
    }

    @Override
    public double getBalance(boolean refresh) {
        if (refresh) {
            BackgroundThreads.rush(() -> {
                NotificationCenter nc = NotificationCenter.getInstance();
                Map<String, Object> info = new HashMap<>();
                info.put("name", getName().toString());
                info.put("address", address);
                // get ERC20 balance
                EthCall erc20GetBalance = queryBalance();
                if (erc20GetBalance == null || erc20GetBalance.hasError()) {
                    nc.postNotification(Wallet.BalanceQueryFailed, this, info);
                } else {
                    BigDecimal result = getBalance(erc20GetBalance);
                    setBalance(result);
                    info.put("balance", result.doubleValue());
                    nc.postNotification(Wallet.BalanceUpdated, this, info);
                }
            });
        }
        return getBalance();
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
            info.put("name", getName().getValue());
            info.put("address", address);
            info.put("to", toAddress);
            info.put("amount", coins);
            // send funds
            Ethereum client = Ethereum.getInstance();
            EthSendTransaction tx = client.sendTransaction(credentials, toAddress, contractAddress,
                    toBalance(coins), gasPrice, gasLimit);
            if (tx == null || tx.hasError()) {
                info.put("balance", balance);
                nc.postNotification(Wallet.TransactionError, this, info);
            } else {
                // TODO: process receipt
                double remaining = balance - coins;
                setBalance(remaining);
                info.put("balance", remaining);
                info.put("transaction", tx);
                nc.postNotification(Wallet.TransactionSuccess, this, info);
            }
        });
        return true;
    }
}
