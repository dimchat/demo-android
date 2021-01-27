package chat.dim.sechat.wallet;

import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import chat.dim.ethereum.Ethereum;
import chat.dim.mkm.ETHAddress;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.sechat.profile.ProfileViewModel;
import chat.dim.threading.BackgroundThreads;
import chat.dim.utils.Log;
import chat.dim.wallet.WalletName;

public class WalletViewModel extends ProfileViewModel {

    static private double gasPrice = 58.0;  // Gwei

    static private double getGasPrice() {
        BackgroundThreads.wait(() -> {
            Ethereum client = Ethereum.getInstance();
            EthGasPrice ethGasPrice = client.ethGasPrice();
            if (ethGasPrice == null || ethGasPrice.hasError()) {
                return;
            }
            BigInteger price = ethGasPrice.getGasPrice();
            if (price == null || price.longValue() <= 0) {
                return;
            }
            BigDecimal wei = new BigDecimal(price);
            BigDecimal gWei = Convert.fromWei(wei, Convert.Unit.GWEI);
            gasPrice = gWei.doubleValue();
            Log.info("new gas price: " + gWei.toString());
        });
        return gasPrice;
    }
    public double getGasPrice(WalletName name) {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return 0;
        }
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            return getGasPrice();
        }
        return 0;
    }

    public long getGasLimit(WalletName name) {
        ID identifier = getIdentifier();
        if (identifier == null) {
            return 0;
        }
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            if (name.equals(WalletName.ETH)) {
                return 21_000;
            } else {
                return 60_000;
            }
        }
        return 0;
    }
}
