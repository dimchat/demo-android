package chat.dim.sechat.wallet.transfer;

import org.web3j.protocol.core.methods.response.EthGasPrice;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

import chat.dim.ethereum.Ethereum;
import chat.dim.mkm.plugins.ETHAddress;
import chat.dim.protocol.Address;
import chat.dim.sechat.wallet.WalletViewModel;
import chat.dim.threading.BackgroundThreads;
import chat.dim.utils.Log;
import chat.dim.wallet.WalletName;

class TransferViewModel extends WalletViewModel {

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
    double getGasPrice(WalletName name) {
        Address address = identifier.getAddress();
        if (address instanceof ETHAddress) {
            return getGasPrice();
        }
        return 0;
    }

    long getGasLimit(WalletName name) {
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
