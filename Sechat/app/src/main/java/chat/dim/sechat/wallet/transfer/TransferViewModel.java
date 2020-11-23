package chat.dim.sechat.wallet.transfer;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import chat.dim.ethereum.ERC20Wallet;
import chat.dim.ethereum.ETHWallet;
import chat.dim.mkm.plugins.ETHAddress;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.sechat.profile.ProfileViewModel;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletFactory;
import chat.dim.wallet.WalletName;

public class TransferViewModel extends ProfileViewModel {

    public static double getGasPrice(String name, ID identifier, boolean refresh) {
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            BigInteger gasPrice = null;
            Wallet wallet = WalletFactory.getWallet(WalletName.fromString(name), address.toString());
            if (wallet instanceof ETHWallet) {
                gasPrice = ((ETHWallet) wallet).gasPrice;
            } else if (wallet instanceof ERC20Wallet) {
                gasPrice = ((ERC20Wallet) wallet).gasPrice;
            }
            if (gasPrice != null) {
                return Convert.fromWei(new BigDecimal(gasPrice), Convert.Unit.GWEI).doubleValue();
            }
        }
        return 58.0;
    }
    public double getGasPrice(String name, boolean refresh) {
        return getGasPrice(name, identifier, refresh);
    }

    public static long getGasLimit(String name, ID identifier, boolean refresh) {
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            BigInteger gasLimit = null;
            Wallet wallet = WalletFactory.getWallet(WalletName.fromString(name), address.toString());
            if (wallet instanceof ETHWallet) {
                gasLimit = ((ETHWallet) wallet).gasLimit;
            } else if (wallet instanceof ERC20Wallet) {
                gasLimit = ((ERC20Wallet) wallet).gasLimit;
            }
            if (gasLimit != null) {
                return gasLimit.longValue();
            }
        }
        return 21000;
    }
    public long getGasLimit(String name, boolean refresh) {
        return getGasLimit(name, identifier, refresh);
    }
}
