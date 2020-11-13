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

import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ChatboxActivity;
import chat.dim.ui.image.ImageViewerActivity;

public class ProfileFragment extends Fragment implements Observer {

    private ProfileViewModel mViewModel;

    private ID identifier;

    private ImageView imageView;
    private TextView nameView;
    private TextView addressView;

    private Button messageButton;
    private Button removeButton;
    private Button addButton;

    public ProfileFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.ContactsUpdated);
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.ContactsUpdated);
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
        nameView = view.findViewById(R.id.nameView);
        addressView = view.findViewById(R.id.addressView);

        messageButton = view.findViewById(R.id.sendMessage);
        removeButton = view.findViewById(R.id.removeContact);
        addButton = view.findViewById(R.id.addContact);

        imageView.setOnClickListener(v -> showAvatar());

        messageButton.setOnClickListener(v -> startChat());
        removeButton.setOnClickListener(v -> removeContact());
        addButton.setOnClickListener(v -> addContact());

        return view;
    }

    private void refresh() {
        Bitmap avatar = mViewModel.getAvatar();
        imageView.setImageBitmap(avatar);

        nameView.setText(mViewModel.getName());
        addressView.setText(mViewModel.getAddressString());

        if (mViewModel.existsContact(identifier)) {
            messageButton.setVisibility(View.VISIBLE);
            removeButton.setVisibility(View.VISIBLE);
            addButton.setVisibility(View.GONE);
        } else {
            messageButton.setVisibility(View.GONE);
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
