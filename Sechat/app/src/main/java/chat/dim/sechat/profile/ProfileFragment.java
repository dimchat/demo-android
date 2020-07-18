package chat.dim.sechat.profile;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
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
import chat.dim.sechat.SechatApp;
import chat.dim.sechat.chatbox.ChatboxActivity;

public class ProfileFragment extends Fragment {

    private Facebook facebook = Facebook.getInstance();

    private ProfileViewModel mViewModel;

    private ID identifier;

    private ImageView imageView;
    private TextView seedView;
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

        imageView = view.findViewById(R.id.imageView);
        seedView = view.findViewById(R.id.seedView);
        addressView = view.findViewById(R.id.addressView);
        numberView = view.findViewById(R.id.numberView);

        addButton = view.findViewById(R.id.addContact);
        messageButton = view.findViewById(R.id.sendMessage);

        addButton.setOnClickListener(v -> {
            addContact(identifier);
            startChat(identifier);
        });
        messageButton.setOnClickListener(v -> startChat(identifier));

        return view;
    }

    private void addContact(ID identifier) {
        User user = facebook.getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user not set");
        }
        facebook.addContact(identifier, user.identifier);
    }

    private void startChat(ID identifier) {
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

        Uri avatar = mViewModel.getAvatarUrl(identifier);
        if (avatar == null) {
            avatar = SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher);
        }
        imageView.setImageURI(avatar);

        seedView.setText(identifier.name);
        addressView.setText(identifier.address.toString());
        numberView.setText(facebook.getNumberString(identifier));

        if (mViewModel.existsContact(identifier)) {
            addButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.VISIBLE);
        } else {
            addButton.setVisibility(View.VISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }
    }

}
