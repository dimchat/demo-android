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

public class ERC20Convert {

    public static BigDecimal fromAbraham(BigInteger number) {
        return from(number, Unit.USDT);
    }
    public static BigInteger toAbraham(double coins) {
        BigDecimal money = to(new BigDecimal(coins), Unit.USDT);
        return money.toBigInteger();
    }

    public static BigDecimal fromAlbert(BigInteger number) {
        return from(number, Unit.DIMT);
    }
    public static BigInteger toAlbert(double coins) {
        BigDecimal money = to(new BigDecimal(coins), Unit.DIMT);
        return money.toBigInteger();
    }

    //-------- converters

    private static BigDecimal from(BigInteger number, Unit unit) {
        return from(new BigDecimal(number), unit);
    }
    private static BigDecimal from(BigDecimal number, Unit unit) {
        return number.divide(unit.factor, 6, BigDecimal.ROUND_HALF_UP);
    }
    private static BigDecimal to(BigDecimal number, Unit unit) {
        return number.multiply(unit.factor);
    }

    public enum Unit {
        //
        //  Units of USDT
        //
        ABRAHAM("abraham", 0),  // 0.000001 USDT
        USDT("usdt", 6),

        //
        //  Units of DIMT
        //
        ALBERT("albert", 0),    // 0.000000000000000001 DIMT
        DIMT("dimt", 18);

        public final String name;
        public final BigDecimal factor;

        Unit(String name, int factor) {
            this.name = name;
            this.factor = BigDecimal.TEN.pow(factor);
        }
    }
}
