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
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.EthCall;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

class Ethereum {
    private static final Ethereum ourInstance = new Ethereum();

    static Ethereum getInstance() {
        return ourInstance;
    }

    private final String API_URL = "https://mainnet.infura.io/v3/";
    private final String API_KEY = "dde1df04b8d4424f8cb09a403f76db1c";
    private final Web3j web3;
    private boolean connected = false;

    static final BigDecimal THE_18TH_POWER_OF_10 = new BigDecimal("1000000000000000000");
    static final BigDecimal THE_6TH_POWER_OF_10 = new BigDecimal("1000000");

    private Ethereum() {
        web3 = Web3j.build(new HttpService(API_URL + API_KEY));
    }

    private boolean connectToEthNetwork() throws IOException {
        if (connected) {
            return true;
        }
        System.out.println("Connecting to Ethereum network with key: " + API_KEY + "...");
        Web3ClientVersion clientVersion = web3.web3ClientVersion().send();
        if (clientVersion == null) {
            System.out.println("failed to connect " + API_URL + API_KEY);
        } else if (clientVersion.hasError()) {
            System.out.println(clientVersion.getError().getMessage());
        } else {
            System.out.println(clientVersion.getWeb3ClientVersion());
            System.out.println("Connected!");
            connected = true;
        }
        return connected;
    }

    BigInteger ethGetBalance(String address) {
        EthGetBalance balance = null;
        try {
            if (connectToEthNetwork()) {
                balance = web3.ethGetBalance(address, DefaultBlockParameterName.LATEST).send();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (balance == null) {
            return null;
        }
        return balance.getBalance();
    }

    TransactionReceipt sendFunds(String fromAddress, String toAddress, double money) {
        // TODO: send funds
        return null;
    };

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    String erc20GetBalance(String address, String contractAddress) {
        Function function = new Function("balanceOf",
                Arrays.asList(new Address(address)),
                Arrays.asList(new TypeReference<Address>() {}));
        String encode = FunctionEncoder.encode(function);
        Transaction tx = Transaction.createEthCallTransaction(address, contractAddress, encode);
        EthCall call = null;
        try {
            if (connectToEthNetwork()) {
                call = web3.ethCall(tx, DefaultBlockParameterName.LATEST).send();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (call == null) {
            return null;
        }
        return call.getValue();
    }
}
