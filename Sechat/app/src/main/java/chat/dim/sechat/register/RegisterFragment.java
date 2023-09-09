package chat.dim.sechat.register;

import androidx.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;

import chat.dim.GlobalVariable;
import chat.dim.Register;
import chat.dim.SharedFacebook;
import chat.dim.SharedMessenger;
import chat.dim.crypto.SignKey;
import chat.dim.digest.MD5;
import chat.dim.format.Hex;
import chat.dim.format.PortableNetworkFile;
import chat.dim.http.FileTransfer;
import chat.dim.mkm.User;
import chat.dim.model.Configuration;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.Visa;
import chat.dim.sechat.R;
import chat.dim.sechat.account.AccountViewModel;
import chat.dim.ui.Alert;
import chat.dim.ui.image.Images;
import chat.dim.ui.web.WebViewActivity;

public class RegisterFragment extends Fragment {

    private AccountViewModel mViewModel;

    private Bitmap avatarImage = null;
    private ImageView imageView;
    private EditText nicknameEditText;
    private CheckBox checkBox;
    private TextView terms;
    private Button okBtn;
    private TextView importBtn;

    public static RegisterFragment newInstance() {
        return new RegisterFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register_fragment, container, false);

        // register new account
        imageView = view.findViewById(R.id.imageView);
        nicknameEditText = view.findViewById(R.id.nickname);
        checkBox = view.findViewById(R.id.checkBox);
        terms = view.findViewById(R.id.terms);
        okBtn = view.findViewById(R.id.okBtn);
        importBtn = view.findViewById(R.id.importBtn);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        // TODO: Use the ViewModel

        imageView.setOnClickListener(v -> showImagePicker());
        terms.setOnClickListener(v -> showTerms());
        okBtn.setOnClickListener(v -> {
            try {
                register();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        importBtn.setOnClickListener(v -> showImportPage());

        checkUser();
    }

    private void checkUser() {
        RegisterActivity activity = (RegisterActivity) getActivity();
        assert activity != null : "failed to get register activity";

        ID identifier = mViewModel.getIdentifier();
        if (identifier == null) {
            //Alert.tips(activity, R.string.register_account_error);
            return;
        }
        activity.close();
    }

    private void showImagePicker() {
        RegisterActivity activity = (RegisterActivity) getActivity();
        assert activity != null : "failed to get register activity";

        if (!checkBox.isChecked()) {
            Alert.tips(activity, R.string.register_agree);
            return;
        }

        activity.startImagePicker();
    }

    private void showImportPage() {
        RegisterActivity activity = (RegisterActivity) getActivity();
        assert activity != null : "failed to get register activity";

        if (!checkBox.isChecked()) {
            Alert.tips(activity, R.string.register_agree);
            return;
        }

        activity.showImportPage();
    }

    private void showTerms() {
        Configuration config = Configuration.getInstance();
        String url = config.getTermsURL();
        String title = (String) getText(R.string.terms);
        WebViewActivity.open(getActivity(), title, url);
    }

    private void register() throws IOException {
        RegisterActivity activity = (RegisterActivity) getActivity();
        assert activity != null : "failed to get register activity";
        if (!activity.tryLaunch()) {
            Alert.tips(activity, R.string.error_permission);
            return;
        }

        if (avatarImage == null) {
            Alert.tips(activity, R.string.register_avatar);
            //return;
        }

        String nickname = nicknameEditText.getText().toString();
        if (nickname.length() == 0) {
            Alert.tips(activity, R.string.register_nickname_hint);
            return;
        }

        if (!checkBox.isChecked()) {
            Alert.tips(activity, R.string.register_agree);
            return;
        }

        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;

        // 1. create user
        Register userRegister = new Register(facebook.getDatabase());
        ID uid = userRegister.createUser(nickname, null);
        if (uid == null) {
            Alert.tips(activity, R.string.register_failed);
            return;
        }
        User user = facebook.getUser(uid);
        Meta meta = user.getMeta();
        Visa visa = user.getVisa();

        boolean ok = true;

        // 2. upload avatar
        if (avatarImage != null) {
            byte[] imageData = Images.jpeg(avatarImage);
            String filename = Hex.encode(MD5.digest(imageData)) + ".jpeg";
            // TODO: upload delegate
            URL url = getFileTransfer().uploadAvatar(imageData, filename, user.getIdentifier());
            if (url == null) {
                // waiting for avatar uploaded
                ok = false;
            } else {
                visa.setAvatar(PortableNetworkFile.parse(url.toString()));
            }
            SignKey sKey = facebook.getPrivateKeyForVisaSignature(user.getIdentifier());
            assert sKey != null : "failed to get private key: " + user.getIdentifier();
            visa.sign(sKey);
            facebook.saveDocument(visa);
        }

        // 3. set current user
        facebook.setCurrentUser(user);

        // 4. upload meta & visa to DIM station
        if (ok) {
            SharedMessenger messenger = shared.messenger;
            messenger.postDocument(visa, meta);
        }

        // 5. show main activity
        checkUser();
    }

    private FileTransfer getFileTransfer() {
        if (ftp == null) {
            ftp = FileTransfer.getInstance();
            Configuration config = Configuration.getInstance();
            ftp.api = config.getUploadURL();
            ftp.secret = config.getMD5Secret();
        }
        return ftp;
    }
    private FileTransfer ftp = null;

    void fetchImage(Bitmap bitmap) {
        if (bitmap != null) {
            avatarImage = bitmap;
            imageView.setImageBitmap(avatarImage);
        }
    }
}
