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
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import chat.dim.notification.NotificationCenter;
import chat.dim.protocol.Address;
import chat.dim.threading.BackgroundThreads;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public abstract class ERC20Wallet implements Wallet {

    private final Credentials credentials;
    private final String address;
    private final String contractAddress;

    private BigInteger gasPrice = new BigInteger("58000000000");  // wei
    private BigInteger gasLimit = new BigInteger("60000");

    ERC20Wallet(Credentials credentials, Address contractAddress) {
        super();
        this.credentials = credentials;
        this.address = credentials.getAddress();
        this.contractAddress = contractAddress.toString();
    }
    ERC20Wallet(Address address, Address contractAddress) {
        super();
        this.credentials = null;
        this.address = address.toString();
        this.contractAddress = contractAddress.toString();
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
    static private final Map<WalletName, Map<String, BigDecimal>> allBalances = new HashMap<>();  // Unit: ERC20 contract

    private double getBalance() {
        Map<String, BigDecimal> balances = allBalances.get(getName());
        if (balances == null) {
            return -1;
        }
        BigDecimal balance = balances.get(address);
        if (balance == null) {
            return -1;
        }
        return balance.doubleValue();
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

    @Override
    public double getBalance(boolean refresh) {
        if (refresh) {
            BackgroundThreads.rush(() -> {
                String event;
                Map<String, Object> info = new HashMap<>();
                info.put("name", getName().getValue());
                info.put("address", address);
                // get ERC20 balance
                Ethereum client = Ethereum.getInstance();
                EthCall erc20GetBalance = client.getBalance(address, contractAddress);
                if (erc20GetBalance == null || erc20GetBalance.hasError()) {
                    event = Wallet.BalanceQueryFailed;
                } else {
                    event = Wallet.BalanceUpdated;
                    BigDecimal balance = getBalance(erc20GetBalance);
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
            info.put("name", getName().getValue());
            info.put("address", address);
            info.put("to", toAddress.toString());
            info.put("amount", coins);
            // send funds
            Ethereum client = Ethereum.getInstance();
            EthSendTransaction tx = client.sendTransaction(credentials, toAddress.toString(), contractAddress,
                    toBalance(coins), gasPrice, gasLimit);
            if (tx == null || tx.hasError()) {
                event = Wallet.TransactionError;
                info.put("balance", balance);
            } else {
                event = Wallet.TransactionSuccess;
                info.put("balance", balance - coins);
                info.put("transactionHash", tx.getTransactionHash());
            }
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(event, this, info);
        });
        return true;
    }
}
