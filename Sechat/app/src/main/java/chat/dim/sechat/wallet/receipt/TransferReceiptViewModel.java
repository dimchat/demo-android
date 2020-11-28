package chat.dim.sechat.wallet.receipt;

import androidx.lifecycle.ViewModel;

import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import chat.dim.ethereum.ETHWallet;
import chat.dim.wallet.Wallet;

public class TransferReceiptViewModel extends ViewModel {

    Transaction getTransactionByHash(Wallet wallet, String txHash) {
        if (wallet instanceof ETHWallet) {
            return ((ETHWallet) wallet).getTransactionByHash(txHash);
        }
        return null;
    }

    TransactionReceipt getTransactionReceipt(Wallet wallet, String txHash) {
        if (wallet instanceof ETHWallet) {
            return ((ETHWallet) wallet).getTransactionReceipt(txHash);
        }
        return null;
    }
}
