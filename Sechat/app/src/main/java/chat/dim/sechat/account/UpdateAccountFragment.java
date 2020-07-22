package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModelProviders;
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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.network.FtpServer;
import chat.dim.sechat.R;
import chat.dim.ui.Images;

public class UpdateAccountFragment extends Fragment {

    private AccountViewModel mViewModel;

    private ImageButton avatarButton;
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

        avatarButton = view.findViewById(R.id.avatar);
        nicknameText = view.findViewById(R.id.nickname);
        numberView = view.findViewById(R.id.number);
        addressView = view.findViewById(R.id.address);

        saveButton = view.findViewById(R.id.save);
        exportButton = view.findViewById(R.id.export);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);

        // avatar
        Uri avatar = mViewModel.getAvatarUrl();
        avatarButton.setImageURI(avatar);

        UpdateAccountActivity activity = (UpdateAccountActivity) getActivity();
        assert activity != null : "should not happen";
        avatarButton.setOnClickListener(v -> activity.startImagePicker());

        // nickname
        String nickname = mViewModel.getNickname();
        nicknameText.setText(nickname);
        if (nickname != null) {
            getActivity().setTitle(nickname);
        }

        // ID.number & address
        String number = mViewModel.getNumberString();
        numberView.setText(number);
        ID identifier = mViewModel.getIdentifier();
        if (identifier != null) {
            addressView.setText(identifier.address.toString());
        }

        saveButton.setOnClickListener(v -> save());
        exportButton.setOnClickListener(v -> export());
    }

    public void setAvatarImage(Bitmap bitmap) {
        avatarImage = bitmap;
        if (bitmap != null) {
            avatarButton.setImageBitmap(avatarImage);
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
    }

    private void export() {
        // TODO: export user info with private key
    }
}
