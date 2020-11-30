package chat.dim.sechat.wallet;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Map;

import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.threading.MainThread;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public class WalletFragment extends Fragment implements Observer {

    private WalletViewModel mViewModel;

    private ID identifier;

    private TextView addressView;

    private TextView ethBalance;
    private TextView usdtBalance;
    private TextView dimtBalance;

    public WalletFragment() {
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
            if (mViewModel.matchesWalletAddress(address)) {
                MainThread.call(() -> refreshPage(false));
            }
            System.out.println("balance updated: " + info);
        }
    }

    public static WalletFragment newInstance(ID identifier) {
        WalletFragment fragment = new WalletFragment();
        fragment.identifier = identifier;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wallet_fragment, container, false);

        addressView = view.findViewById(R.id.addressView);

        ethBalance = view.findViewById(R.id.ethBalance);
        usdtBalance = view.findViewById(R.id.usdtBalance);
        dimtBalance = view.findViewById(R.id.dimtBalance);

        return view;
    }

    private void refreshPage(boolean queryBalance) {
        mViewModel.setBalance(ethBalance, WalletName.ETH, queryBalance);
        mViewModel.setBalance(usdtBalance, WalletName.USDT_ERC20, queryBalance);
        mViewModel.setBalance(dimtBalance, WalletName.DIMT, queryBalance);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(WalletViewModel.class);
        // TODO: Use the ViewModel

        mViewModel.setIdentifier(identifier);

        addressView.setText(mViewModel.getAddressString());

        refreshPage(true);
    }

}
