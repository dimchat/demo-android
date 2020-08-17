package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.network.FtpServer;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.Alert;
import chat.dim.ui.image.Images;

public class UpdateAccountFragment extends Fragment {

    private AccountViewModel mViewModel;

    private CardView cardView;

    private ImageView avatarView;
    private EditText nicknameText;
    private TextView numberView;
    private TextView addressView;

    private Bitmap avatarImage = null;

    private Button saveButton;
    private Button exportButton;

    public static UpdateAccountFragment newInstance() {
        return new UpdateAccountFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.update_account_fragment, container, false);

        cardView = view.findViewById(R.id.cardView);

        avatarView = view.findViewById(R.id.avatarView);
        nicknameText = view.findViewById(R.id.nickname);
        numberView = view.findViewById(R.id.number);
        addressView = view.findViewById(R.id.address);

        saveButton = view.findViewById(R.id.save);
        exportButton = view.findViewById(R.id.export);

        // hide keyboard
        nicknameText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                SechatApp.getInstance().hideKeyboard(nicknameText);
            }
        });
        LinearLayout scrollView = view.findViewById(R.id.linearLayout);
        scrollView.setOnClickListener(v -> nicknameText.clearFocus());

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);

        // avatar
        Bitmap avatar = mViewModel.getAvatar();
        avatarView.setImageBitmap(avatar);

        UpdateAccountActivity activity = (UpdateAccountActivity) getActivity();
        assert activity != null : "should not happen";
        cardView.setOnClickListener(v -> activity.startImagePicker());

        // nickname
        String nickname = mViewModel.getNickname();
        nicknameText.setText(nickname);
        if (nickname != null) {
            getActivity().setTitle(nickname);
        }

        // ID.number & address
        numberView.setText(mViewModel.getNumberString());
        addressView.setText(mViewModel.getAddressString());

        saveButton.setOnClickListener(v -> save());
        exportButton.setOnClickListener(v -> activity.exportAccount(mViewModel));
    }

    public void setAvatarImage(Bitmap bitmap) {
        avatarImage = bitmap;
        if (bitmap != null) {
            avatarView.setImageBitmap(avatarImage);
        }
    }

    private void save() {
        ID identifier = mViewModel.getIdentifier();
        if (identifier == null) {
            throw new NullPointerException("current user ID empty");
        }
        Profile profile = mViewModel.getProfile();
        if (profile == null) {
            profile = new UserProfile(identifier);
        }

        // upload avatar
        if (avatarImage != null) {
            FtpServer ftp = FtpServer.getInstance();
            byte[] imageData = Images.jpeg(avatarImage);
            if (imageData != null) {
                String avatarURL = ftp.uploadAvatar(imageData, identifier);
                if (profile instanceof UserProfile) {
                    ((UserProfile) profile).setAvatar(avatarURL);
                } else {
                    profile.setProperty("avatar", avatarURL);
                }
            }
        }

        // update nickname
        String nickname = nicknameText.getText().toString();
        if (nickname.length() > 0) {
            profile.setName(nickname);
        }

        mViewModel.updateProfile(profile);

        Alert.tips(getActivity(), R.string.account_saved);
    }
}
