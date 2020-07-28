package chat.dim.sechat.register;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.extension.Register;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.model.Configuration;
import chat.dim.model.Facebook;
import chat.dim.network.FtpServer;
import chat.dim.sechat.Client;
import chat.dim.sechat.MainActivity;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.Alert;
import chat.dim.ui.ImagePickerActivity;
import chat.dim.ui.Images;
import chat.dim.ui.Resources;
import chat.dim.ui.WebViewActivity;

public class RegisterActivity extends ImagePickerActivity {

    private Bitmap avatarImage = null;
    private ImageView imageView;
    private EditText nicknameEditText;
    private CheckBox checkBox;
    private TextView terms;
    private Button okBtn;

    public RegisterActivity() {
        super();
        setCrop(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        SechatApp.launch(getApplication(), this);

        setTitle(R.string.register);

        Client client = Client.getInstance();
        User user = client.getCurrentUser();
        if (user == null) {
            // register new account
            imageView = findViewById(R.id.imageView);
            nicknameEditText = findViewById(R.id.nickname);
            checkBox = findViewById(R.id.checkBox);
            terms = findViewById(R.id.terms);
            okBtn = findViewById(R.id.okBtn);

            imageView.setOnClickListener(v -> startImagePicker());
            terms.setOnClickListener(v -> showTerms());
            okBtn.setOnClickListener(v -> register());
        } else {
            // OK
            close();
        }
    }

    private void close() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void showTerms() {
        Configuration config = Configuration.getInstance();
        open(R.string.terms, config.getTermsURL());
    }

    private void open(int resId, String url) {
        String title = (String) Resources.getText(this, resId);
        open(title, url);
    }

    private void open(String title, String url) {
        Intent intent = new Intent();
        intent.setClass(this, WebViewActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("URL", url);
        startActivity(intent);
    }

    private void register() {
        if (!SechatApp.launch(getApplication(), this)) {
            Alert.tips(this, R.string.error_permission);
            return;
        }

        if (avatarImage == null) {
            Alert.tips(this, R.string.register_avatar);
            //return;
        }

        String nickname = nicknameEditText.getText().toString();
        if (nickname.length() == 0) {
            Alert.tips(this, R.string.register_nickname_hint);
            return;
        }

        if (!checkBox.isChecked()) {
            Alert.tips(this, R.string.register_agree);
            return;
        }

        // 1. create user
        Register userRegister = new Register();
        User user = userRegister.createUser(nickname, null);
        if (user == null) {
            Alert.tips(this, R.string.register_failed);
            return;
        }
        Meta meta = user.getMeta();
        Profile profile = user.getProfile();

        // 2. upload avatar
        if (avatarImage != null) {
            FtpServer ftp = FtpServer.getInstance();
            byte[] imageData = Images.jpeg(avatarImage);
            if (imageData != null) {
                String avatarURL = ftp.uploadAvatar(imageData, user.identifier);
                if (profile instanceof UserProfile) {
                    ((UserProfile) profile).setAvatar(avatarURL);
                } else {
                    profile.setProperty("avatar", avatarURL);
                }
            }
        }

        // 3. set current user
        Facebook facebook = Facebook.getInstance();
        facebook.setCurrentUser(user);

        // 4. upload meta & profile to DIM station
        userRegister.upload(user.identifier, meta, profile);

        // 5. show main activity
        close();
    }

    //
    //  ImagePickerActivity
    //

    @Override
    protected String getTemporaryDirectory() {
        return Paths.appendPathComponent(ExternalStorage.root, "tmp");
    }

    @Override
    protected void fetchImage(Bitmap bitmap) {
        if (bitmap != null) {
            avatarImage = bitmap;
            imageView.setImageBitmap(avatarImage);
        }
    }
}
