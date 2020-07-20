package chat.dim.sechat.register;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.IOException;

import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.extension.Register;
import chat.dim.filesys.ExternalStorage;
import chat.dim.mkm.plugins.UserProfile;
import chat.dim.model.Facebook;
import chat.dim.network.FtpServer;
import chat.dim.sechat.Client;
import chat.dim.sechat.MainActivity;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.Alert;
import chat.dim.ui.ImagePicker;
import chat.dim.ui.Resources;

public class RegisterActivity extends AppCompatActivity {

    private Bitmap avatarImage = null;
    private ImageButton imageButton;
    private EditText nicknameEditText;
    private CheckBox checkBox;
    private Button okBtn;

    private final ImagePicker imagePicker;

    public RegisterActivity() {
        super();
        imagePicker = new ImagePicker(this);
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
            imageButton = findViewById(R.id.imageButton);
            nicknameEditText = findViewById(R.id.nickname);
            checkBox = findViewById(R.id.checkBox);
            okBtn = findViewById(R.id.okBtn);

            imageButton.setOnClickListener(v -> imagePicker.start());
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

    private void fetchAvatar(Intent data) {
        avatarImage = imagePicker.getBitmap(data);
        if (avatarImage != null) {
            imageButton.setImageBitmap(avatarImage);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == ImagePicker.RequestCode.Album.value) {
            assert data != null : "Intent data should not be empty";
            Uri source = data.getData();
            if (source == null) {
                // no data
                return;
            }
            String dir = Resources.appendPathComponent(ExternalStorage.root, "tmp");
            try {
                if (imagePicker.cropPicture(source, dir)) {
                    // waiting for crop
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            // CROP not support
            fetchAvatar(data);
            return;
        } else if (requestCode == ImagePicker.RequestCode.Crop.value) {
            assert data != null : "Intent data should not be empty";
            fetchAvatar(data);
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void register() {
        if (!SechatApp.launch(getApplication(), this)) {
            Alert.tips(this, R.string.error_permission);
            return;
        }

        if (avatarImage == null) {
            Alert.tips(this, R.string.register_avatar);
            return;
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
        FtpServer ftp = FtpServer.getInstance();
        byte[] imageData = ImagePicker.compressJPEG(avatarImage);
        if (imageData != null) {
            String avatarURL = ftp.uploadAvatar(imageData, user.identifier);
            if (profile instanceof UserProfile) {
                ((UserProfile) profile).setAvatar(avatarURL);
            } else {
                profile.setProperty("avatar", avatarURL);
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
}
