package chat.dim.sechat.profile;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ChatboxActivity;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.ui.image.ImageViewerActivity;

public class ProfileFragment extends Fragment {

    private Facebook facebook = Facebook.getInstance();

    private ProfileViewModel mViewModel;

    private ID identifier;

    private ImageView imageView;
    private TextView nameView;
    private TextView addressView;
    private TextView numberView;

    private Button addButton;
    private Button messageButton;

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
        numberView = view.findViewById(R.id.numberView);

        addButton = view.findViewById(R.id.addContact);
        messageButton = view.findViewById(R.id.sendMessage);

        imageView.setOnClickListener(v -> showAvatar());

        addButton.setOnClickListener(v -> addContact());
        messageButton.setOnClickListener(v -> startChat());

        return view;
    }

    private void showAvatar() {
        Uri avatar = UserViewModel.getAvatarUri(identifier);
        if (avatar != null) {
            ImageViewerActivity.show(getActivity(), avatar, UserViewModel.getUsername(identifier));
        }
    }

    private void addContact() {
        User user = facebook.getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user not set");
        }
        facebook.addContact(identifier, user.identifier);

        // open chat box
        startChat();
    }

    private void startChat() {
        assert getContext() != null : "fragment context error";
        Intent intent = new Intent();
        intent.setClass(getContext(), ChatboxActivity.class);
        intent.putExtra("ID", identifier.toString());
        startActivity(intent);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ProfileViewModel.class);
        // TODO: Use the ViewModel

        Bitmap avatar = UserViewModel.getAvatar(identifier);
        imageView.setImageBitmap(avatar);

        nameView.setText(UserViewModel.getNickname(identifier));
        addressView.setText(EntityViewModel.getAddressString(identifier));
        numberView.setText(EntityViewModel.getNumberString(identifier));

        if (mViewModel.existsContact(identifier)) {
            addButton.setVisibility(View.GONE);
            messageButton.setVisibility(View.VISIBLE);
        } else {
            addButton.setVisibility(View.VISIBLE);
            messageButton.setVisibility(View.GONE);
        }
    }

}
