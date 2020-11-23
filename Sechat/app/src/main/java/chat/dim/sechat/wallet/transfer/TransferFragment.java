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
import android.widget.EditText;
import android.widget.TextView;

import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.wallet.Wallet;

public class TransferFragment extends Fragment implements Observer {

    private TransferViewModel mViewModel;

    private ID identifier;
    private String wallet;

    private TextView balanceView;
    private EditText toAddress;
    private EditText amountView;

    private TextView feeView;
    private EditText priceView;
    private EditText limitView;

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
                balanceView.setText(mViewModel.getBalance(wallet, false));
            }
            System.out.println("balance " + info.get("name")
                    + ": " + mViewModel.getBalance(wallet, false));
        }
    }

    public static TransferFragment newInstance(ID identifier, String wallet) {
        TransferFragment fragment = new TransferFragment();
        fragment.identifier = identifier;
        fragment.wallet = wallet;
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

        balanceView.setText(mViewModel.getBalance(wallet, true));
        toAddress.setText(identifier.getAddress().toString());

        double gasPrice = mViewModel.getGasPrice(wallet, true);
        priceView.setText(String.format(Locale.CHINA, "%.2f", gasPrice));
        long gasLimit = mViewModel.getGasLimit(wallet, true);
        limitView.setText(String.format(Locale.CHINA, "%d", gasLimit));
        calculateFee();
    }

    private void calculateFee() {
        try {
            BigDecimal price = new BigDecimal(priceView.getText().toString());
            BigDecimal limit = new BigDecimal(limitView.getText().toString());
            BigDecimal wei = Convert.toWei(price.multiply(limit), Convert.Unit.GWEI);
            BigDecimal fee = Convert.fromWei(wei, Convert.Unit.ETHER);
            String text = String.format(Locale.CHINA, "%.2f Gwei * %d = %.6f ETH", price.floatValue(), limit.intValue(), fee.doubleValue());
            feeView.setText(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
