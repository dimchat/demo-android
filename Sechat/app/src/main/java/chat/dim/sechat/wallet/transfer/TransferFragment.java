package chat.dim.sechat.wallet.transfer;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.web3j.utils.Convert;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import chat.dim.Entity;
import chat.dim.User;
import chat.dim.ethereum.ERC20Wallet;
import chat.dim.ethereum.ETHWallet;
import chat.dim.mkm.plugins.ETHAddress;
import chat.dim.model.Facebook;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.wallet.WalletViewModel;
import chat.dim.sechat.wallet.receipt.TransferReceiptActivity;
import chat.dim.ui.Alert;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public class TransferFragment extends Fragment implements Observer {

    private WalletViewModel mViewModel;

    private ID identifier;
    private WalletName walletName;

    private TextView balanceView;
    private EditText toAddress;
    private EditText amountView;

    private TextView feeView;
    private EditText priceView;
    private EditText limitView;

    private Button transferButton;

    public TransferFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, Wallet.BalanceUpdated);
        nc.addObserver(this, Wallet.TransactionSuccess);
        nc.addObserver(this, Wallet.TransactionError);
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, Wallet.BalanceUpdated);
        nc.removeObserver(this, Wallet.TransactionSuccess);
        nc.removeObserver(this, Wallet.TransactionError);
        super.onDestroy();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(Wallet.BalanceUpdated)) {
            String address = (String) info.get("address");
            if (mViewModel.matchesWalletAddress(address)) {
                mViewModel.setBalance(balanceView, walletName, false);
            }
            System.out.println("balance updated: " + info);
        } else if (name.equals(Wallet.TransactionSuccess)) {
            Message msg = new Message();
            msg.what = 9527;
            msg.obj = info;
            msgHandler.sendMessage(msg);
        } else if (name.equals(Wallet.TransactionError)) {
            Message msg = new Message();
            msg.what = 9528;
            msgHandler.sendMessage(msg);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @SuppressWarnings("unchecked")
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 9527: {
                    showDetail((HashMap<String, Object>) msg.obj);
                    break;
                }
                case 9528: {
                    Alert.tips(getContext(), R.string.transfer_failed);
                    transferButton.setEnabled(true);
                    break;
                }
            }
        }
    };

    public static TransferFragment newInstance(ID identifier, WalletName walletName) {
        TransferFragment fragment = new TransferFragment();
        fragment.identifier = identifier;
        fragment.walletName = walletName;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transfer_fragment, container, false);

        balanceView = view.findViewById(R.id.balance);
        toAddress = view.findViewById(R.id.toAddress);
        amountView = view.findViewById(R.id.amount);

        feeView = view.findViewById(R.id.feeView);
        priceView = view.findViewById(R.id.gasPrice);
        limitView = view.findViewById(R.id.gasLimit);

        transferButton = view.findViewById(R.id.transferButton);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateFee();
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        };
        priceView.addTextChangedListener(watcher);
        limitView.addTextChangedListener(watcher);

        transferButton.setOnClickListener(v -> {
            if (checkTransfer()) {
                doTransfer();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(WalletViewModel.class);
        // TODO: Use the ViewModel

        Facebook facebook = Facebook.getInstance();
        User user = facebook.getCurrentUser();
        mViewModel.setIdentifier(user.identifier);

        mViewModel.setBalance(balanceView, walletName, true);

        toAddress.setText(identifier.getAddress().toString());

        double gasPrice = mViewModel.getGasPrice(walletName);
        priceView.setText(String.format(Locale.CHINA, "%.2f", gasPrice));
        long gasLimit = mViewModel.getGasLimit(walletName);
        limitView.setText(String.format(Locale.CHINA, "%d", gasLimit));
        calculateFee();
    }

    private double getAmount() {
        try {
            String amount = amountView.getText().toString();
            return Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }
    private double getGasPrice() {
        try {
            String price = priceView.getText().toString();
            return Double.parseDouble(price);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }
    private long getGasLimit() {
        try {
            String limit = limitView.getText().toString();
            return Long.parseLong(limit);
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void calculateFee() {
        double price = getGasPrice();
        long limit = getGasLimit();
        BigDecimal wei = Convert.toWei(BigDecimal.valueOf(price), Convert.Unit.GWEI);
        BigDecimal fee = BigDecimal.valueOf(limit).multiply(wei);
        BigDecimal ether = Convert.fromWei(fee, Convert.Unit.ETHER);
        String text = String.format(Locale.CHINA, "%.2f Gwei * %d = %.6f ETH", price, limit, ether.doubleValue());
        feeView.setText(text);
    }

    private boolean checkTransfer() {
        // check amount & balance
        double amount = getAmount();
        if (amount < 0.01) {
            Alert.tips(getContext(), "Amount too small");
            return false;
        }
        Wallet wallet = mViewModel.getWallet(walletName);
        double balance = wallet.getBalance(true);
        if (balance < amount) {
            Alert.tips(getContext(), R.string.insufficient_funds);
            return false;
        }
        // TODO: check gas price & limit
        double gasPrice = getGasPrice();
        if (gasPrice < 2) {
            Alert.tips(getContext(), "Gas price too low");
            return false;
        }
        long gasLimit = getGasLimit();
        if (gasLimit < 21000) {
            Alert.tips(getContext(), "Gas limit too low");
            return false;
        }
        // check receiver address
        ID receiver = Entity.parseID(toAddress.getText().toString());
        if (receiver == null) {
            Alert.tips(getContext(), "Address error");
            return false;
        } else if (receiver.getAddress() instanceof ETHAddress) {
            return true;
        } else {
            Alert.tips(getContext(), "Only transfer to ETH address now");
            return false;
        }
    }

    private void doTransfer() {
        double gasPrice = getGasPrice();
        long gasLimit = getGasLimit();
        Wallet wallet = mViewModel.getWallet(walletName);
        if (wallet instanceof ETHWallet) {
            ((ETHWallet) wallet).setGasPrice(gasPrice);
            ((ETHWallet) wallet).setGasLimit(gasLimit);
        } else if (wallet instanceof ERC20Wallet) {
            ((ERC20Wallet) wallet).setGasPrice(gasPrice);
            ((ERC20Wallet) wallet).setGasLimit(gasLimit);
        } else {
            Alert.tips(getContext(), "wallet error");
            return;
        }
        ID receiver = Entity.parseID(toAddress.getText().toString());
        double amount = getAmount();
        boolean ok = wallet.transfer(receiver.getAddress().toString(), amount);
        if (ok) {
            transferButton.setEnabled(false);
            Alert.tips(getContext(), R.string.transfer_sent);
        } else {
            Alert.tips(getContext(), R.string.transfer_failed);
        }
    }

    private void showDetail(Map<String, Object> info) {
        info.put("walletName", walletName.getValue());
        info.put("walletAddress", identifier.getAddress().toString());

        FragmentActivity activity = getActivity();
        assert activity != null : "failed to get context";

        Intent intent = new Intent();
        intent.setClass(activity, TransferReceiptActivity.class);
        intent.putExtra("info", (Serializable) info);
        activity.startActivity(intent);

        activity.finish();
    }
}
