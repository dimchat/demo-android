/* license: https://mit-license.org
 *
 *  Ming-Ke-Ming : Decentralized User Identity Authentication
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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
package chat.dim;

import java.util.Locale;

import chat.dim.format.Base58;
import chat.dim.format.Hex;
import chat.dim.mkm.BTCAddress;
import chat.dim.mkm.ETHAddress;
import chat.dim.protocol.Address;
import chat.dim.protocol.EntityType;
import chat.dim.protocol.ID;

public final class Anonymous {

    public static String getName(ID identifier) {
        String name = identifier.getName();
        if (name == null || name.length() == 0) {
            name = getName(identifier.getType());
        }
        return name + " (" + getNumberString(identifier.getAddress()) + ")";
    }
    private static String getName(int type) {
        if (type == EntityType.BOT.value) {
            return "Bot";
        } else if (type == EntityType.STATION.value) {
            return "Station";
        } else if (type == EntityType.ISP.value) {
            return "ISP";
        }
        if (EntityType.isUser(type)) {
            return "User";
        } else if (EntityType.isGroup(type)) {
            return "Group";
        }
        return "Unknown";
    }

    public static String getNumberString(Address address) {
        long number = getNumber(address);
        String string = String.format(Locale.CHINA, "%010d", number);
        return string.substring(0, 3) + "-"
                + string.substring(3, 6) + "-"
                + string.substring(6);
    }

    public static long getNumber(Address address) {
        if (address instanceof BTCAddress) {
            return btcNumber(address.toString());
        }
        if (address instanceof ETHAddress) {
            return ethNumber(address.toString());
        }
        throw new RuntimeException("address error: " + address);
    }

    private static long btcNumber(String address) {
        byte[] data = Base58.decode(address);
        return userNumber(data);
    }
    private static long ethNumber(String address) {
        byte[] data = Hex.decode(address.substring(2));
        return userNumber(data);
    }

    private static long userNumber(byte[] cc) {
        int len = cc.length;
        return (long)
                (cc[len-4] & 0xFF) << 24 |
                (cc[len-3] & 0xFF) << 16 |
                (cc[len-2] & 0xFF) << 8 |
                (cc[len-1] & 0xFF);
    }
}
