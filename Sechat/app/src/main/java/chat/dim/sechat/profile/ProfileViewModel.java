package chat.dim.sechat.profile;

import android.graphics.Color;
import android.net.Uri;
import android.widget.TextView;

import org.web3j.crypto.Credentials;

import java.util.List;
import java.util.Locale;

import chat.dim.User;
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

    Uri getAvatarUri() {
        ID identifier = getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("ID not set");
        }
        String avatar = getFacebook().getAvatar(identifier);
        if (avatar == null) {
            return null;
        }
        return Uri.parse(avatar);
    }

    boolean containsContact(ID contact) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        List<ID> contacts = getFacebook().getContacts(user.identifier);
        if (contacts == null) {
            return false;
        }
        return contacts.contains(contact);
    }

    //
    //  ETH wallets
    //
    public boolean matchesWalletAddress(String address) {
        return getIdentifier().getAddress().toString().equalsIgnoreCase(address);
    }

    public Wallet getWallet(WalletName name) {
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
