package chat.dim.sechat.wallet.receipt;

import androidx.lifecycle.ViewModelProviders;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.web3j.protocol.core.methods.response.Transaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;

import chat.dim.Entity;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.Address;
import chat.dim.sechat.R;
import chat.dim.ui.Alert;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletFactory;
import chat.dim.wallet.WalletName;

public class TransferReceiptFragment extends Fragment implements Observer {

    private TransferReceiptViewModel mViewModel;

    private Wallet wallet;

    private Map<String, Object> info;

    private TextView statusView;

    private TextView amountView;
    private TextView feeView;
    private TextView gasView;
    private TextView fromView;
    private TextView toView;

    private TextView hashView;

    private Button refreshButton;

    public TransferReceiptFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, Wallet.TransactionSuccess);
        nc.addObserver(this, Wallet.TransactionError);
        nc.addObserver(this, Wallet.TransactionWaiting);
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, Wallet.TransactionSuccess);
        nc.removeObserver(this, Wallet.TransactionError);
        nc.removeObserver(this, Wallet.TransactionWaiting);
        super.onDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(Wallet.TransactionSuccess)) {
            this.info = info;
            Message msg = new Message();
            msg.what = 9527;
            msgHandler.sendMessage(msg);
        } else if (name.equals(Wallet.TransactionError)) {
            Message msg = new Message();
            msg.what = 9528;
            msgHandler.sendMessage(msg);
        } else if (name.equals(Wallet.TransactionWaiting)) {
            Message msg = new Message();
            msg.what = 9529;
            msgHandler.sendMessage(msg);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9527: {
                    refreshPage();
                    break;
                }
                case 9528: {
                    Alert.tips(getContext(), R.string.transfer_failed);
                    break;
                }
                case 9529: {
                    Alert.tips(getContext(), "Waiting...");
                    break;
                }
            }
        }
    };

    public static TransferReceiptFragment newInstance(Map<String, Object> info) {
        WalletName walletName = WalletName.fromString((String) info.get("walletName"));
        Address walletAddress = Entity.parseID(info.get("walletAddress")).getAddress();

        TransferReceiptFragment fragment = new TransferReceiptFragment();
        fragment.info = info;
        fragment.wallet = WalletFactory.getWallet(walletName, walletAddress);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transfer_receipt_fragment, container, false);

        statusView = view.findViewById(R.id.statusView);

        amountView = view.findViewById(R.id.amountView);
        feeView = view.findViewById(R.id.feeView);
        gasView = view.findViewById(R.id.gasView);
        fromView = view.findViewById(R.id.fromView);
        toView = view.findViewById(R.id.toView);

        hashView = view.findViewById(R.id.hashView);

        refreshButton = view.findViewById(R.id.refreshButton);

        refreshButton.setOnClickListener(v -> refreshPage());

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(TransferReceiptViewModel.class);
        // TODO: Use the ViewModel

        refreshPage();
    }

    private void refreshPage() {
        if (info == null) {
            return;
        }

        String txHash = (String) info.get("transactionHash");
        if (txHash == null) {
            Alert.tips(getContext(), "Failed to get transaction hash");
            return;
        }
        hashView.setText(txHash);

        Transaction tx = mViewModel.getTransactionByHash(wallet, txHash);
        TransactionReceipt receipt = mViewModel.getTransactionReceipt(wallet, txHash);
        refreshPage(tx, receipt);
    }
    private void refreshPage(Transaction tx, TransactionReceipt receipt) {
        if (tx == null || receipt == null) {
            return;
        }

        BigInteger value = tx.getValue();
        BigInteger price = tx.getGasPrice();
        BigInteger gas = receipt.getGasUsed();

        String from = tx.getFrom();
        String to = tx.getTo();

        BigDecimal ether = Convert.fromWei(new BigDecimal(value), Convert.Unit.ETHER);
        amountView.setText(String.format(Locale.CHINA, "%s ETH", ether.toString()));

        BigDecimal fee = Convert.fromWei(new BigDecimal(price.multiply(gas)), Convert.Unit.ETHER);
        feeView.setText(String.format(Locale.CHINA, "%s ETH", fee.toString()));

        BigDecimal gwei = Convert.fromWei(new BigDecimal(price), Convert.Unit.GWEI);
        gasView.setText(String.format(Locale.CHINA, "GasPrice(%.02f Gwei) * Gas(%d)", gwei.doubleValue(), gas.longValue()));

        fromView.setText(from);
        toView.setText(to);

        if (receipt.isStatusOK()) {
            statusView.setText(R.string.success);
            refreshButton.setEnabled(false);
            refreshButton.setVisibility(View.GONE);
        } else {
            statusView.setText(R.string.tx_waiting);
            refreshButton.setEnabled(true);
            refreshButton.setVisibility(View.VISIBLE);
        }
    }
}
