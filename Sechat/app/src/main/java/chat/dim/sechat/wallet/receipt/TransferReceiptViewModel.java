package chat.dim.sechat.wallet.receipt;

import androidx.lifecycle.ViewModel;

import org.web3j.protocol.core.methods.response.EthGetTransactionReceipt;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import chat.dim.ethereum.DIMTWallet;
import chat.dim.ethereum.ERC20Convert;
import chat.dim.ethereum.ERC20Wallet;
import chat.dim.ethereum.ETHWallet;
import chat.dim.ethereum.Ethereum;
import chat.dim.ethereum.USDTWallet;
import chat.dim.notification.NotificationCenter;
import chat.dim.threading.BackgroundThreads;
import chat.dim.wallet.Wallet;

public class TransferReceiptViewModel extends ViewModel {

    //
    //  Memory caches for Transaction
    //
    static private final Map<String, Transaction> transactions = new HashMap<>();
    static private final Map<String, TransactionReceipt> receipts = new HashMap<>();

    static private Transaction getTransaction(String txHash) {
        return transactions.get(txHash);
    }
    static private void setTransaction(Transaction tx) {
        transactions.put(tx.getHash(), tx);
    }
    static private TransactionReceipt getReceipt(String txHash) {
        return receipts.get(txHash);
    }
    static private void setReceipt(TransactionReceipt receipt) {
        receipts.put(receipt.getTransactionHash(), receipt);
    }

    public Transaction getTransactionByHash(String txHash) {
        Transaction tx = getTransaction(txHash);
        if (tx == null) {
            BackgroundThreads.rush(() -> {
                String event;
                Map<String, Object> info = new HashMap<>();
                info.put("transactionHash", txHash);
                // send funds
                Ethereum client = Ethereum.getInstance();
                EthTransaction ethTransaction = client.ethGetTransactionByHash(txHash);
                if (ethTransaction == null || ethTransaction.hasError()) {
                    event = Wallet.TransactionError;
                } else {
                    Transaction result = ethTransaction.getResult();
                    String blockHash = result.getBlockHash();
                    if (Numeric.toBigInt(blockHash).compareTo(BigInteger.ZERO) != 0) {
                        event = Wallet.TransactionSuccess;
                        setTransaction(result);
                        info.put("transactionHash", result.getHash());
                    } else {
                        event = Wallet.TransactionWaiting;
                    }
                }
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(event, this, info);
            });
        }
        return tx;
    }

    public TransactionReceipt getTransactionReceipt(String txHash) {
        TransactionReceipt receipt = getReceipt(txHash);
        if (receipt == null) {
            BackgroundThreads.rush(() -> {
                String event;
                Map<String, Object> info = new HashMap<>();
                info.put("transactionHash", txHash);
                // send funds
                Ethereum client = Ethereum.getInstance();
                EthGetTransactionReceipt getReceipt = client.ethGetTransactionReceipt(txHash);
                if (getReceipt == null || getReceipt.hasError()) {
                    event = Wallet.TransactionError;
                } else {
                    TransactionReceipt result = getReceipt.getResult();
                    if (result.isStatusOK()) {
                        event = Wallet.TransactionSuccess;
                        setReceipt(result);
                        info.put("transactionHash", result.getTransactionHash());
                    } else {
                        event = Wallet.TransactionWaiting;
                    }
                }
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(event, this, info);
            });
        }
        return receipt;
    }

    String getAmount(Wallet wallet, Transaction tx, TransactionReceipt receipt) {
        if (wallet instanceof ETHWallet) {
            BigInteger value = tx.getValue();
            BigDecimal ether = Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER);
            return String.format(Locale.CHINA, "%,f ETH", ether.doubleValue());
        } else if (wallet instanceof ERC20Wallet) {
            List<Log> logs = receipt.getLogs();
            if (logs != null && logs.size() > 0) {
                String hex = logs.get(0).getData();
                BigInteger value = Numeric.toBigInt(hex);
                if (wallet instanceof USDTWallet) {
                    BigDecimal coins = ERC20Convert.fromMicroUSDT(value);
                    return String.format(Locale.CHINA, "%,f USDT", coins.doubleValue());
                } else if (wallet instanceof DIMTWallet) {
                    BigDecimal coins = ERC20Convert.fromAlbert(value);
                    return String.format(Locale.CHINA, "%,f DIMT", coins.doubleValue());
                }
            }
        }
        return null;
    }

    String getFromAddress(Wallet wallet, Transaction tx, TransactionReceipt receipt) {
        return tx.getFrom();
    }

    String getToAddress(Wallet wallet, Transaction tx, TransactionReceipt receipt) {
        if (wallet instanceof ERC20Wallet) {
            List<Log> logs = receipt.getLogs();
            if (logs != null && logs.size() > 0) {
                List<String> topics = logs.get(0).getTopics();
                if (topics != null && topics.size() > 2) {
                    String to = topics.get(2);
                    if (to.length() > 42) {
                        return "0x" + to.substring(to.length() - 40);
                    }
                }
            }
        }
        return tx.getTo();
    }
}
