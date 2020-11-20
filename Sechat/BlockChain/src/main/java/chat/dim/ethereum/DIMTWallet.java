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

import java.math.BigDecimal;
import java.math.BigInteger;

public class DIMTWallet extends ERC20Wallet {

    public DIMTWallet(String address) {
//        super(address, "0x042B6b20b09749125F969820Eca69e75893abFCA");
        super(address, "0x81246a3F5fab7Aa9f4F625866105F3CAfFc67686");
    }

    @Override
    protected double getBalance(String erc20Balance) {
        if (erc20Balance.startsWith("0x")) {
            erc20Balance = erc20Balance.substring(2);
        }
        if (erc20Balance.length() == 0) {
            return 0;
        }
        BigInteger balance = new BigInteger(erc20Balance, 16);
        BigDecimal amount = new BigDecimal(balance);
        BigDecimal res = amount.divide(Ethereum.THE_18TH_POWER_OF_10, 18, BigDecimal.ROUND_DOWN);
        return res.doubleValue();
    }
}
