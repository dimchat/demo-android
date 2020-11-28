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

import org.web3j.protocol.core.methods.response.TransactionReceipt;

import java.math.BigInteger;
import java.util.Locale;
import java.util.Map;

import chat.dim.Entity;
import chat.dim.ethereum.ETHWallet;
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
    private TextView timeView;

    private TextView amountView;
    private TextView feeView;
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
        timeView = view.findViewById(R.id.timeView);

        amountView = view.findViewById(R.id.amountView);
        feeView = view.findViewById(R.id.feeView);
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

        TransactionReceipt receipt = (TransactionReceipt) info.get("receipt");
        if (receipt == null) {
            String blockHash = (String) info.get("blockHash");

            if (wallet instanceof ETHWallet) {
                receipt = ((ETHWallet) wallet).getTransactionReceipt(txHash, blockHash);
            }
        }
        refreshPage(receipt);
    }
    private void refreshPage(TransactionReceipt receipt) {
        if (receipt == null) {
            return;
        }

        BigInteger gas = receipt.getGasUsed();
        feeView.setText(String.format(Locale.CHINA, "%d", gas.longValue()));
        fromView.setText(receipt.getFrom());
        toView.setText(receipt.getTo());

        if (receipt.isStatusOK()) {
            statusView.setText("Success");
            refreshButton.setEnabled(false);
        } else {
            statusView.setText("Error");
        }
    }
}
