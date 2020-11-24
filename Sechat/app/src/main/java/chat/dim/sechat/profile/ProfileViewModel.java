package chat.dim.sechat.profile;

import android.net.Uri;

import org.web3j.crypto.Credentials;

import java.util.List;

import chat.dim.User;
import chat.dim.crypto.PrivateKey;
import chat.dim.format.Hex;
import chat.dim.mkm.plugins.ETHAddress;
import chat.dim.model.Facebook;
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
    //  ETH wallets
    //
    private static Wallet getETHWallet(WalletName name, ID identifier) {
        Facebook facebook = Facebook.getInstance();
        PrivateKey privateKey = (PrivateKey) facebook.getPrivateKeyForSignature(identifier);
        if (privateKey == null) {
            return WalletFactory.getWallet(name, identifier.getAddress().toString());
        } else {
            byte[] keyData = privateKey.getData();
            Credentials account = Credentials.create(Hex.encode(keyData));
            return WalletFactory.getWallet(name, account);
        }
    }
    public Wallet getWallet(WalletName name) {
        if (identifier.getAddress() instanceof ETHAddress) {
            return getETHWallet(name, identifier);
        }
        // only support ETH address now
        return null;
    }
    public Wallet getWallet(String name) {
        return getWallet(WalletName.fromString(name));
    }
}
