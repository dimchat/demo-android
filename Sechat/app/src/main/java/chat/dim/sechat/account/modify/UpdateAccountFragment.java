package chat.dim.sechat.account.modify;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.cardview.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import chat.dim.mkm.plugins.UserProfile;
import chat.dim.network.FtpServer;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.ID;
import chat.dim.protocol.Profile;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.sechat.account.AccountViewModel;
import chat.dim.ui.Alert;
import chat.dim.ui.image.Images;

public class UpdateAccountFragment extends Fragment implements DialogInterface.OnClickListener {

    private AccountViewModel mViewModel;

    private CardView cardView;

    private ImageView avatarView;
    private EditText nicknameText;
    private TextView addressView;

    private Bitmap avatarImage = null;

    private Button saveButton;
    private Button exportButton;
    private Button deleteButton;

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
        addressView = view.findViewById(R.id.address);

        saveButton = view.findViewById(R.id.save);
        exportButton = view.findViewById(R.id.export);
        deleteButton = view.findViewById(R.id.delete);

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
        addressView.setText(mViewModel.getAddressString());

        saveButton.setOnClickListener(v -> save());
        exportButton.setOnClickListener(v -> activity.exportAccount(mViewModel));
        deleteButton.setOnClickListener(v -> deleteAccount());
    }

    void setAvatarImage(Bitmap bitmap) {
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
        Profile profile = mViewModel.getProfile(Profile.BIO);
        assert profile != null : "profile object should not be null: " + identifier;

        // upload avatar
        if (avatarImage != null) {
            FtpServer ftp = FtpServer.getInstance();
            byte[] imageData = Images.jpeg(avatarImage);
            String avatarURL = ftp.uploadAvatar(imageData, identifier);
            if (profile instanceof UserProfile) {
                ((UserProfile) profile).setAvatar(avatarURL);
            } else {
                profile.setProperty("avatar", avatarURL);
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

    private void deleteAccount() {
        FragmentActivity activity = getActivity();
        assert activity != null : "failed to get fragment activity";
        CharSequence[] items = {
                "DON'T!!",
                "DON'T!!",
                "DON'T!!",
                "Yes, I had already saved private key.",
                "DON'T!!",
                "DON'T!!",
                "DON'T!!",
        };
        Alert.alert(activity, items, this);
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (which == 3) {
            ID identifier = mViewModel.removeCurrentUser();
            Map<String, Object> info = new HashMap<>();
            info.put("ID", identifier);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.AccountDeleted, this, info);
            Alert.tips(getContext(), "Current user removed!");
        }
    }
}
