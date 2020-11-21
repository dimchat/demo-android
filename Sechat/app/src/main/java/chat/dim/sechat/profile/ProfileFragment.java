package chat.dim.sechat.profile;

import androidx.lifecycle.ViewModelProviders;
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

import chat.dim.User;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ChatboxActivity;
import chat.dim.ui.Alert;
import chat.dim.ui.image.ImageViewerActivity;
import chat.dim.wallet.Wallet;
import chat.dim.wallet.WalletName;

public class ProfileFragment extends Fragment implements Observer {

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
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.ContactsUpdated)) {
            ID contact = (ID) info.get("ID");
            if (identifier.equals(contact)) {
                refresh();
            }
        } else if (name.equals(Wallet.BalanceUpdated)) {
            String address = (String) info.get("address");
            if (identifier.getAddress().toString().equals(address)) {
                ethBalance.setText(mViewModel.getBalance(WalletName.ETH, false));
                usdtBalance.setText(mViewModel.getBalance(WalletName.USDT_ERC20, false));
                dimtBalance.setText(mViewModel.getBalance(WalletName.DIMT, false));
            }
            System.out.println("balance " + info.get("name")
                    + ": " + mViewModel.getBalance((String) info.get("name"), false));
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

        messageButton.setOnClickListener(v -> startChat());
        remitButton.setOnClickListener(v -> remitMoney());
        removeButton.setOnClickListener(v -> removeContact());
        addButton.setOnClickListener(v -> addContact());

        return view;
    }

    private void refresh() {
        Bitmap avatar = mViewModel.getAvatar();
        imageView.setImageBitmap(avatar);

        addressView.setText(mViewModel.getAddressString());

        ethBalance.setText(mViewModel.getBalance(WalletName.ETH, true));
        usdtBalance.setText(mViewModel.getBalance(WalletName.USDT_ERC20, true));
        dimtBalance.setText(mViewModel.getBalance(WalletName.DIMT, true));

        if (mViewModel.existsContact(identifier)) {
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
            String name = mViewModel.getUsername();
            ImageViewerActivity.show(getActivity(), avatar, name);
        }
    }

    private void addContact() {
        mViewModel.addContact(identifier);
        // open chat box
        startChat();
    }

    private void removeContact() {
        mViewModel.removeContact(identifier);
        close();
    }

    private void startChat() {
        assert getContext() != null : "fragment context error";
        Intent intent = new Intent();
        intent.setClass(getContext(), ChatboxActivity.class);
        intent.putExtra("ID", identifier.toString());
        startActivity(intent);
    }

    private void remitMoney() {
        User user = ProfileViewModel.getCurrentUser();
        if (user == null) {
            throw new NullPointerException("failed to get current user");
        }
        if (user.identifier.equals(identifier)) {
            Alert.tips(getContext(), R.string.remit_self);
            return;
        }
        Alert.tips(getContext(), "Coming soon!");
    }

    private void close() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            // should not happen
            return;
        }
        activity.finish();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ProfileViewModel.class);

        mViewModel.setIdentifier(identifier);
        mViewModel.refreshProfile();

        refresh();
    }
}
