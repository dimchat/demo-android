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
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

public class Ethereum {
    private static final Ethereum ourInstance = new Ethereum();

    public static Ethereum getInstance() {
        return ourInstance;
    }

    private final String API_URL = "https://mainnet.infura.io/v3/";
    private final String API_KEY = "dde1df04b8d4424f8cb09a403f76db1c";
    private final Web3j web3j;
    private boolean connected = false;

    private Ethereum() {
        web3j = Web3j.build(new HttpService(API_URL + API_KEY));
    }

    private Web3ClientVersion connect() {
        try {
            return web3j.web3ClientVersion().send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    private boolean offline() {
        synchronized (Ethereum.class) {
            if (connected) {
                return false;
            }
            System.out.println("Connecting to Ethereum network with key: " + API_KEY + "...");
            Web3ClientVersion clientVersion = connect();
            if (clientVersion == null) {
                System.out.println("failed to connect " + API_URL + API_KEY);
            } else if (clientVersion.hasError()) {
                System.out.println(clientVersion.getError().getMessage());
            } else {
                System.out.println(clientVersion.getWeb3ClientVersion());
                System.out.println("Connected!");
                connected = true;
            }
            return !connected;
        }
    }

    /**
     *  Get current gas price from network
     *
     * @return gas price in wei
     */
    public EthGasPrice ethGasPrice() {
        if (offline()) {
            return null;
        }
        try {
            return web3j.ethGasPrice().send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Get current balance with ETH address
     *
     * @param address - ETH address
     * @return balance in wei
     */
    EthGetBalance ethGetBalance(String address) {
        if (offline()) {
            return null;
        }
        try {
            return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Get number of transactions with address
     *
     * @param address - ETH address
     * @return count of transactions
     */
    private EthGetTransactionCount ethGetTransactionCount(String address) {
        if (offline()) {
            return null;
        }
        try {
            return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Get transaction by hash
     *
     * @param txHash - transaction hash
     * @return null on failed
     */
    EthTransaction ethGetTransactionByHash(String txHash) {
        if (offline()) {
            return null;
        }
        try {
            return web3j.ethGetTransactionByHash(txHash).send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Get transaction receipt by hash
     *
     * @param txHash - transaction hash
     * @return null on failed
     */
    EthGetTransactionReceipt ethGetTransactionReceipt(String txHash) {
        if (offline()) {
            return null;
        }
        try {
            return web3j.ethGetTransactionReceipt(txHash).send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Send amount of money from address-1 to address-2
     *
     * @param fromAccount - account with private key
     * @param toAddress   - receiver's ETH address
     * @param sum         - amount of ETH in wei
     * @param gasPrice    - gas price
     * @param gasLimit    - gas limit
     * @return null on failed
     */
    EthSendTransaction ethSendTransaction(Credentials fromAccount, String toAddress, BigInteger sum,
                                          BigInteger gasPrice, BigInteger gasLimit) {
        if (offline()) {
            return null;
        }
        // get transaction count as next nonce
        EthGetTransactionCount count = ethGetTransactionCount(fromAccount.getAddress());
        BigInteger nonce = count == null ? BigInteger.ZERO : count.getTransactionCount();
        RawTransaction tx = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, sum);
        byte[] signed = TransactionEncoder.signMessage(tx, fromAccount);
        String hex = Numeric.toHexString(signed);
        try {
            return web3j.ethSendRawTransaction(hex).send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     *  Send amount of money from address-1 to address-2
     *
     * @param fromAccount - account with private key
     * @param toAddress   - receiver's ETH address
     * @param eth         - amount of ETH in ether
     * @return null on failed
     */
    TransactionReceipt sendFunds(Credentials fromAccount, String toAddress, BigDecimal eth) {
        if (offline()) {
            return null;
        }
        try {
            return Transfer.sendFunds(web3j, fromAccount, toAddress, eth, Convert.Unit.ETHER).send();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private EthCall ethCall(Transaction tx) {
        if (offline()) {
            return null;
        }
        try {
            return web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //
    //  ERC20
    //

    /**
     *  Get current balance with ETH address and contract
     *
     * @param address         - ETH address
     * @param contractAddress - ERC20 contract
     * @return balance in smallest unit (as string value)
     */
    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    EthCall getBalance(String address, String contractAddress) {
        Function function = new Function("balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Address>() {}));
        String encode = FunctionEncoder.encode(function);
        Transaction tx = Transaction.createEthCallTransaction(address, contractAddress, encode);
        return ethCall(tx);
    }

    /**
     *  Send amount of money (ERC20) from address-1 to address-2
     *
     * @param fromAccount     - account with private key
     * @param toAddress       - receiver's ERC20 address
     * @param contractAddress - ERC20 contract address
     * @param sum             - amount of money in smallest unit
     * @param gasPrice        - gas price
     * @param gasLimit        - gas limit
     * @return null on failed
     */
    EthSendTransaction sendTransaction(Credentials fromAccount, String toAddress, String contractAddress, BigInteger sum,
                                       BigInteger gasPrice, BigInteger gasLimit) {
        if (offline()) {
            return null;
        }
        String encode = FunctionEncoder.encode(new Function("transfer",
                Arrays.asList(new Address(toAddress), new Uint256(sum)),
                Collections.emptyList()));
        RawTransactionManager manager = new RawTransactionManager(web3j, fromAccount);
        try {
            return manager.sendTransaction(gasPrice, gasLimit, contractAddress, encode, BigInteger.ZERO);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
