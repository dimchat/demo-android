package chat.dim.sechat.wallet.receipt;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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

import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.Address;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.threading.MainThread;
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
            MainThread.call(this::refreshPage);
        } else if (name.equals(Wallet.TransactionError)) {
            MainThread.call(() -> Alert.tips(getContext(), R.string.transfer_failed));
        } else if (name.equals(Wallet.TransactionWaiting)) {
            MainThread.call(() -> Alert.tips(getContext(), "Waiting..."));
        }
    }

    public static TransferReceiptFragment newInstance(Map<String, Object> info) {
        WalletName walletName = WalletName.fromString((String) info.get("walletName"));
        Address walletAddress = ID.parse(info.get("walletAddress")).getAddress();

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

        Transaction tx = mViewModel.getTransactionByHash(txHash);
        TransactionReceipt receipt = mViewModel.getTransactionReceipt(txHash);
        refreshPage(tx, receipt);
    }
    private void refreshPage(Transaction tx, TransactionReceipt receipt) {
        if (tx == null || receipt == null) {
            return;
        }

        String amount = mViewModel.getAmount(wallet, tx, receipt);
        amountView.setText(amount);

        BigInteger price = tx.getGasPrice();
        BigInteger gas = receipt.getGasUsed();

        BigDecimal fee = Convert.fromWei(new BigDecimal(price.multiply(gas)), Convert.Unit.ETHER);
        feeView.setText(String.format(Locale.CHINA, "%s ETH", fee.toString()));

        BigDecimal gwei = Convert.fromWei(new BigDecimal(price), Convert.Unit.GWEI);
        gasView.setText(String.format(Locale.CHINA, "GasPrice(%.02f Gwei) * Gas(%,d)", gwei.doubleValue(), gas.longValue()));

        String from = mViewModel.getFromAddress(wallet, tx, receipt);
        String to = mViewModel.getToAddress(wallet, tx, receipt);

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
