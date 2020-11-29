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

import java.math.BigDecimal;
import java.math.BigInteger;

import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.wallet.WalletName;

public class DIMTWallet extends ERC20Wallet {

    static private final String CONTRACT_ADDRESS = "0x81246a3F5fab7Aa9f4F625866105F3CAfFc67686";

    public DIMTWallet(Credentials credentials) {
        super(credentials, ID.parse(CONTRACT_ADDRESS).getAddress());
    }
    public DIMTWallet(Address address) {
        super(address, ID.parse(CONTRACT_ADDRESS).getAddress());
    }

    @Override
    protected WalletName getName() {
        return WalletName.DIMT;
    }

    @Override
    protected BigDecimal getBalance(EthCall erc20Balance) {
        String balance = erc20Balance.getValue();
        if (balance.startsWith("0x")) {
            balance = balance.substring(2);
        }
        BigInteger number = new BigInteger(balance, 16);
        return ERC20Convert.fromAlbert(number);
    }

    @Override
    protected BigInteger toBalance(double coins) {
        return ERC20Convert.toAlbert(coins);
    }
}
