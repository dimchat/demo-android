package chat.dim.sechat.wallet.transfer;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

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
    }

}
