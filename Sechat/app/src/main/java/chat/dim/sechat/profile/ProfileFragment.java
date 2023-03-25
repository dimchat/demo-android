package chat.dim.sechat.profile;

import androidx.lifecycle.ViewModelProviders;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.mkm.User;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.Client;
import chat.dim.sechat.R;
import chat.dim.sechat.wallet.transfer.TransferActivity;
import chat.dim.threading.BackgroundThreads;
import chat.dim.threading.MainThread;
import chat.dim.ui.Alert;
import chat.dim.ui.image.ImageViewerActivity;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public class ProfileFragment extends Fragment implements Observer, DialogInterface.OnClickListener {

    private ProfileViewModel mViewModel;

    private ID identifier;

    private ImageView imageView;
    private TextView addressView;

    private TextView ethBalance;
    private TextView usdtBalance;
    private TextView dimtBalance;

    private Button messageButton;
    private Button remitButton;
    private Button removeButton;
    private Button addButton;

    public ProfileFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.ContactsUpdated);
        nc.addObserver(this, Wallet.BalanceUpdated);
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.ContactsUpdated);
        nc.removeObserver(this, Wallet.BalanceUpdated);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map<String, Object> info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.ContactsUpdated)) {
            ID contact = (ID) info.get("contact");
            if (identifier.equals(contact)) {
                MainThread.call(() -> refreshPage(false));
            }
        } else if (name.equals(Wallet.BalanceUpdated)) {
            String address = (String) info.get("address");
            if (mViewModel.matchesWalletAddress(address)) {
                MainThread.call(() -> refreshPage(false));
            }
            System.out.println("balance updated: " + info);
        }
    }

    public static ProfileFragment newInstance(ID identifier) {
        ProfileFragment fragment = new ProfileFragment();
        fragment.identifier = identifier;
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        imageView = view.findViewById(R.id.avatarView);
        addressView = view.findViewById(R.id.addressView);

        ethBalance = view.findViewById(R.id.ethBalance);
        usdtBalance = view.findViewById(R.id.usdtBalance);
        dimtBalance = view.findViewById(R.id.dimtBalance);

        messageButton = view.findViewById(R.id.sendMessage);
        remitButton = view.findViewById(R.id.remitMoney);
        removeButton = view.findViewById(R.id.removeContact);
        addButton = view.findViewById(R.id.addContact);

        imageView.setOnClickListener(v -> showAvatar());

        messageButton.setOnClickListener(v -> Client.getInstance().startChat(identifier));
        remitButton.setOnClickListener(v -> remitMoney());
        removeButton.setOnClickListener(v -> removeContact());
        addButton.setOnClickListener(v -> addContact());

        BackgroundThreads.wait(() -> {
            GlobalVariable shared = GlobalVariable.getInstance();
            shared.messenger.queryDocument(identifier);
        });

        return view;
    }

    private void close() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            // should not happen
            return;
        }
        activity.finish();
    }

    private void refreshPage(boolean queryBalance) {
        Bitmap avatar = mViewModel.getAvatar();
        imageView.setImageBitmap(avatar);

        addressView.setText(mViewModel.getAddressString());

        mViewModel.setBalance(ethBalance, WalletName.ETH, queryBalance);
        mViewModel.setBalance(usdtBalance, WalletName.USDT_ERC20, queryBalance);
        mViewModel.setBalance(dimtBalance, WalletName.DIMT, queryBalance);

        if (mViewModel.containsContact(identifier)) {
            messageButton.setVisibility(View.VISIBLE);
            remitButton.setVisibility(View.VISIBLE);
            removeButton.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.GONE);
        } else {
            messageButton.setVisibility(View.GONE);
            remitButton.setVisibility(View.GONE);
            removeButton.setVisibility(View.GONE);
            addButton.setVisibility(View.VISIBLE);
        }
    }

    private void showAvatar() {
        Uri avatar = mViewModel.getAvatarUri();
        if (avatar != null) {
            String name = mViewModel.getName();
            ImageViewerActivity.show(getActivity(), avatar, name);
        }
    }

    private void addContact() {
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        User user = facebook.getCurrentUser();
        assert user != null : "failed to get current user";
        boolean ok = facebook.addContact(identifier, user.getIdentifier());
        assert ok : "failed to add contact: " + identifier + ", user: " + user;
        // open chat box
        Client client = Client.getInstance();
        client.startChat(identifier);
    }

    private void removeContact() {
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        User user = facebook.getCurrentUser();
        assert user != null : "failed to get current user";
        boolean ok = facebook.removeContact(identifier, user.getIdentifier());
        assert ok : "failed to remove contact: " + identifier + ", user: " + user;
        close();
    }

    private void remitMoney() {
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        User user = facebook.getCurrentUser();
        assert user != null : "failed to get current user";
        if (user.getIdentifier().equals(identifier)) {
            Alert.tips(getContext(), R.string.remit_self);
            return;
        }
        CharSequence[] items = {
                "ETH",
                "USDT",
                "DIMT",
        };
        Alert.alert(getActivity(), items, this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        String name;
        switch (which) {
            case 0: {
                name = "ETH";
                break;
            }

            case 1: {
                name = "USDT-ERC20";
                break;
            }

            case 2: {
                name = "DIMT";
                break;
            }

            default: {
                return;
            }
        }

        assert getContext() != null : "fragment context error";
        Intent intent = new Intent();
        intent.setClass(getContext(), TransferActivity.class);
        intent.putExtra("ID", identifier.toString());
        intent.putExtra("wallet", name);
        startActivity(intent);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ProfileViewModel.class);
        mViewModel.setIdentifier(identifier);
        mViewModel.refreshDocument();

        refreshPage(true);
    }
}
