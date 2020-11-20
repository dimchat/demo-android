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
package chat.dim.sechat.model;

import androidx.lifecycle.ViewModel;

import java.util.Locale;

import chat.dim.mkm.plugins.ETHAddress;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.protocol.Profile;
import chat.dim.threading.BackgroundThreads;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletFactory;
import chat.dim.wallet.WalletName;

public class EntityViewModel extends ViewModel {

    protected static Facebook facebook = Facebook.getInstance();

    protected ID identifier = null;

    //
    //  ID
    //
    public ID getIdentifier() {
        return identifier;
    }
    public void setIdentifier(ID identifier) {
        this.identifier = identifier;
    }

    //
    //  Address string
    //
    public static String getAddressString(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("entity ID empty");
        }
        return identifier.getAddress().toString();
    }
    public String getAddressString() {
        return getAddressString(getIdentifier());
    }

    //
    //  Name string
    //
    private static String getName(ID identifier, Profile profile) {
        String name = profile.getName();
        if (name != null) {
            return name;
        }
        name = identifier.getName();
        if (name != null) {
            return name;
        }
        return identifier.toString();
    }


    public static String getName(ID identifier) {
        return getName(identifier, getProfile(identifier));
    }
    public String getName() {
        return getName(identifier, getProfile());
    }

    //
    //  Profile
    //
    public static Profile getProfile(ID identifier) {
        if (identifier == null) {
            throw new NullPointerException("entity ID empty");
        }
        return facebook.getProfile(identifier);
    }
    public Profile getProfile() {
        return getProfile(getIdentifier());
    }

    public void refreshProfile() {
        BackgroundThreads.wait(() -> {
            Profile profile = getProfile();
            if (facebook.isEmpty(profile) || facebook.isExpired(profile)) {
                Messenger messenger = Messenger.getInstance();
                messenger.queryProfile(identifier);
            }
        });
    }

    //
    //  ETH
    //
    public static String getBalance(String name, ID identifier, boolean refresh) {
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            Wallet wallet = WalletFactory.getWallet(WalletName.fromString(name), address.toString());
            if (wallet != null) {
                double balance = wallet.getBalance(refresh);
                return String.format(Locale.CHINA, "%.06f", balance);
            }
        }
        return "-";
    }
    public String getBalance(String name, boolean refresh) {
        return getBalance(name, identifier, refresh);
    }
}
