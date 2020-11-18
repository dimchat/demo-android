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
package chat.dim.eth;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.Web3ClientVersion;
import org.web3j.protocol.http.HttpService;

import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

class Ethereum {
    private static final Ethereum ourInstance = new Ethereum();

    static Ethereum getInstance() {
        return ourInstance;
    }

    private final String API_URL = "https://mainnet.infura.io/v3/";
    private final String API_KEY = "dde1df04b8d4424f8cb09a403f76db1c";
    private Web3j web3 = null;

    private Ethereum() {
        System.out.println("Connecting to Ethereum network with key: " + API_KEY + "...");
        Web3ClientVersion clientVersion = connectToEthNetwork();
        if (clientVersion == null) {
            System.out.println("failed to connect " + API_URL + API_KEY);
        } else if (clientVersion.hasError()) {
            System.out.println(clientVersion.getError().getMessage());
        } else {
            System.out.println(clientVersion.getWeb3ClientVersion());
            System.out.println("Connected!");
        }
    }

    private Web3ClientVersion connectToEthNetwork() {
        web3 = Web3j.build(new HttpService(API_URL + API_KEY));
        try {
            return web3.web3ClientVersion().sendAsync().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return null;
        }
    }

    long getBalance(String address) {
        EthGetBalance balance;
        try {
            balance = web3.ethGetBalance(address, DefaultBlockParameter.valueOf("latest")).send();
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
        BigInteger number = balance.getBalance();
        return number.longValue();
    }
}
