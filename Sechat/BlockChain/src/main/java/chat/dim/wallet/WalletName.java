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
package chat.dim.wallet;

import org.jetbrains.annotations.NotNull;

public enum WalletName implements Wallet.Name {
    BTC("btc"),
//    USDT("usdt"),
    USDT_OMNI("usdt-omni"),
    USDT_ERC20("usdt-erc20"),
    ETH ("eth"),
    DIMT("dimt");

    private final String value;

    WalletName(String name) {
        value = name.toLowerCase();
    }

    @Override
    public String getValue() {
        return value;
    }

    @NotNull
    @Override
    public String toString() {
        return value;
    }

    public static WalletName fromString(String name) {
        for (WalletName walletName : values()) {
            if (name.equalsIgnoreCase(walletName.value)) {
                return walletName;
            }
        }
        return valueOf(name);
    }
}
