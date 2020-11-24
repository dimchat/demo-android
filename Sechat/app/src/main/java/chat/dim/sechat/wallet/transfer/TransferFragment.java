package chat.dim.sechat.wallet.transfer;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;
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
import chat.dim.ui.Alert;
import chat.dim.wallet.Wallet;

public class TransferFragment extends Fragment implements Observer {

    private TransferViewModel mViewModel;

    private ID identifier;
    private String walletName;

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
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, Wallet.BalanceUpdated);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(Wallet.BalanceUpdated)) {
            String address = (String) info.get("address");
            if (identifier.getAddress().toString().equals(address)) {
                balanceView.setText(mViewModel.getBalance(walletName, false));
            }
            System.out.println("balance " + info.get("name")
                    + ": " + mViewModel.getBalance(walletName, false));
        }
    }

    public static TransferFragment newInstance(ID identifier, String walletName) {
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
        mViewModel = ViewModelProviders.of(this).get(TransferViewModel.class);
        // TODO: Use the ViewModel

        Facebook facebook = Facebook.getInstance();
        User user = facebook.getCurrentUser();
        mViewModel.setIdentifier(user.identifier);

        balanceView.setText(mViewModel.getBalance(walletName, true));
        toAddress.setText(identifier.getAddress().toString());

        double gasPrice = mViewModel.getGasPrice(walletName, true);
        priceView.setText(String.format(Locale.CHINA, "%.2f", gasPrice));
        long gasLimit = mViewModel.getGasLimit(walletName, true);
        limitView.setText(String.format(Locale.CHINA, "%d", gasLimit));
        calculateFee();
    }

    private double getBalance() {
        String balance = mViewModel.getBalance(walletName, false);
        return new BigDecimal(balance).doubleValue();
    }
    private double getAmount() {
        try {
            String amount = amountView.getText().toString();
            return new BigDecimal(amount).doubleValue();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
    private BigDecimal getGasPrice() {
        try {
            String price = priceView.getText().toString();
            return new BigDecimal(price);
        } catch (Exception e) {
            e.printStackTrace();
            return BigDecimal.ZERO;
        }
    }
    private BigInteger getGasLimit() {
        try {
            String limit = limitView.getText().toString();
            return new BigInteger(limit);
        } catch (Exception e) {
            e.printStackTrace();
            return BigInteger.ZERO;
        }
    }

    private void calculateFee() {
        BigDecimal gasPrice = getGasPrice();
        BigInteger gasLimit = getGasLimit();
        BigDecimal wei = Convert.toWei(gasPrice, Convert.Unit.GWEI);
        BigInteger fee = gasLimit.multiply(wei.toBigInteger());
        BigDecimal ether = Convert.fromWei(new BigDecimal(fee), Convert.Unit.ETHER);
        String text = String.format(Locale.CHINA, "%.2f Gwei * %d = %.6f ETH",
                gasPrice.floatValue(), gasLimit.intValue(), ether.doubleValue());
        feeView.setText(text);
    }

    private boolean checkTransfer() {
        // check amount & balance
        double amount = getAmount();
        if (amount < 0.01) {
            Alert.tips(getContext(), "Amount too small");
            return false;
        }
        double balance = getBalance();
        if (balance < amount) {
            Alert.tips(getContext(), R.string.insufficient_funds);
            return false;
        }
        // TODO: check gas price & limit
        BigDecimal gasPrice = getGasPrice();
        if (gasPrice.doubleValue() < 2) {
            Alert.tips(getContext(), "Gas price too low");
            return false;
        }
        BigInteger gasLimit = getGasLimit();
        if (gasLimit.longValue() < 20000) {
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
        BigDecimal gasPrice = getGasPrice();
        BigInteger gasLimit = getGasLimit();
        BigDecimal price = Convert.toWei(gasPrice, Convert.Unit.GWEI);
        Wallet wallet = mViewModel.getMyWallet(walletName);
        if (wallet instanceof ETHWallet) {
            ((ETHWallet) wallet).gasPrice = price.toBigInteger();
            ((ETHWallet) wallet).gasLimit = gasLimit;
        } else if (wallet instanceof ERC20Wallet) {
            ((ERC20Wallet) wallet).gasPrice = price.toBigInteger();
            ((ERC20Wallet) wallet).gasLimit = gasLimit;
        } else {
            Alert.tips(getContext(), "wallet error");
            return;
        }
        ID receiver = Entity.parseID(toAddress.getText().toString());
        double amount = getAmount();
        boolean ok = wallet.transfer(receiver.getAddress().toString(), amount);
        if (ok) {
            Alert.tips(getContext(), R.string.transfer_sent);
        } else {
            Alert.tips(getContext(), R.string.transfer_failed);
        }
    }
}
