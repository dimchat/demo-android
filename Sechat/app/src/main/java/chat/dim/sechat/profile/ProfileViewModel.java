package chat.dim.sechat.profile;

import android.graphics.Color;
import android.widget.TextView;

import org.web3j.crypto.Credentials;

import java.util.Locale;

import chat.dim.crypto.SignKey;
import chat.dim.format.Hex;
import chat.dim.mkm.ETHAddress;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletFactory;
import chat.dim.wallet.WalletName;

public class ProfileViewModel extends UserViewModel {

    //
    //  ETH wallets
    //
    public boolean matchesWalletAddress(String address) {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return false;
        }
        return identifier.getAddress().toString().equalsIgnoreCase(address);
    }

    public Wallet getWallet(WalletName name) {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return null;
        }
        SignKey sKey = getFacebook().getPrivateKeyForVisaSignature(identifier);
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            if (sKey == null) {
                return WalletFactory.getWallet(name, address);
            } else {
                Credentials account = Credentials.create(Hex.encode(sKey.getData()));
                return WalletFactory.getWallet(name, account);
            }
        }
        // only support ETH address now
        return null;
    }

    public void setBalance(TextView textView, WalletName name, boolean refresh) {
        Wallet wallet = getWallet(name);
        if (wallet == null) {
            textView.setText("-");
            textView.setTextColor(Color.RED);
        } else {
            double balance = wallet.getBalance(refresh);
            textView.setText(String.format(Locale.CHINA, "%,g", balance));
            if (balance < 0) {
                textView.setText("...");
                textView.setTextColor(Color.BLUE);
            } else if (balance > 0) {
                textView.setTextColor(Color.BLACK);
            } else {
                textView.setTextColor(Color.GRAY);
            }
        }
    }
}
