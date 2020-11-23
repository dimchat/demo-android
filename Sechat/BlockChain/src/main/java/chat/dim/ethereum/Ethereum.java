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
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

class Ethereum {
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

    private boolean offline() throws IOException {
        synchronized (Ethereum.class) {
            if (connected) {
                return false;
            }
            System.out.println("Connecting to Ethereum network with key: " + API_KEY + "...");
            Web3ClientVersion clientVersion = web3j.web3ClientVersion().send();
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

    EthGasPrice ethGasPrice() {
        try {
            if (offline()) {
                return null;
            }
            return web3j.ethGasPrice().send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private EthGetTransactionCount ethGetTransactionCount(String address) {
        try {
            if (offline()) {
                return null;
            }
            return web3j.ethGetTransactionCount(address, DefaultBlockParameterName.LATEST).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    EthGetBalance ethGetBalance(String address) {
        try {
            if (offline()) {
                return null;
            }
            return web3j.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    EthCall ethCall(Transaction tx) {
        try {
            if (offline()) {
                return null;
            }
            return web3j.ethCall(tx, DefaultBlockParameterName.LATEST).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    EthSendTransaction ethSendTransaction(Credentials fromAccount, String toAddress,
                                          BigInteger sum,
                                          BigInteger gasPrice, BigInteger gasLimit) {
        EthGetTransactionCount count = ethGetTransactionCount(fromAccount.getAddress());
        BigInteger nonce = count == null ? BigInteger.ZERO : count.getTransactionCount();
        RawTransaction tx = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, sum);
        byte[] signed = TransactionEncoder.signMessage(tx, fromAccount);
        String hex = Numeric.toHexString(signed);
        try {
            if (offline()) {
                return null;
            }
            return web3j.ethSendRawTransaction(hex).send();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    EthSendTransaction sendTransaction(Credentials fromAccount, String toAddress,
                                       String contractAddress, BigInteger sum,
                                       BigInteger gasPrice, BigInteger gasLimit) {
        String encode = FunctionEncoder.encode(new Function("transfer",
                Arrays.asList(new Address(toAddress), new Uint256(sum)),
                Collections.emptyList()));
        RawTransactionManager manager = new RawTransactionManager(web3j, fromAccount);
        try {
            if (offline()) {
                return null;
            }
            return manager.sendTransaction(gasPrice, gasLimit, contractAddress, encode, BigInteger.ZERO);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
