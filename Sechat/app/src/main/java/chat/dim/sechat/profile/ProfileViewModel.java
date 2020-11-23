package chat.dim.sechat.profile;

import android.net.Uri;

import java.util.List;
import java.util.Locale;

import chat.dim.User;
import chat.dim.mkm.plugins.ETHAddress;
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
        String avatar = facebook.getAvatar(identifier);
        if (avatar == null) {
            return null;
        }
        return Uri.parse(avatar);
    }

    boolean existsContact(ID contact) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }
        List<ID> contacts = facebook.getContacts(user.identifier);
        if (contacts == null) {
            return false;
        }
        return contacts.contains(contact);
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
    public String getBalance(WalletName name, boolean refresh) {
        return getBalance(name.getValue(), identifier, refresh);
    }
}
