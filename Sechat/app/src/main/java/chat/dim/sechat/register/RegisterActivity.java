package chat.dim.sechat.register;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import chat.dim.User;
import chat.dim.extension.Register;
import chat.dim.model.Facebook;
import chat.dim.sechat.Client;
import chat.dim.sechat.MainActivity;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;

public class RegisterActivity extends AppCompatActivity {

    private ImageButton imageButton;
    private EditText nicknameEditText;
    private CheckBox checkBox;
    private Button okBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);

        SechatApp.launch(getApplication(), this);

        setTitle(R.string.register);

        imageButton = findViewById(R.id.imageButton);
        nicknameEditText = findViewById(R.id.nickname);
        checkBox = findViewById(R.id.checkBox);
        okBtn = findViewById(R.id.okBtn);

        okBtn.setOnClickListener(v -> register());

        Client client = Client.getInstance();
        User user = client.getCurrentUser();
        if (user != null) {
            close();
        }
    }

    private void close() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    private void alert(int resId) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show();
    }

    private void register() {
        if (!SechatApp.launch(getApplication(), this)) {
            alert(R.string.error_permission);
            return;
        }

        String nickname = nicknameEditText.getText().toString();
        if (nickname.length() == 0) {
            alert(R.string.register_nickname_hint);
            return;
        }

        if (!checkBox.isChecked()) {
            alert(R.string.register_agree);
            return;
        }

        // 1. create user
        Register userRegister = new Register();
        User user = userRegister.createUser(nickname, null);
        if (user == null) {
            alert(R.string.register_failed);
            return;
        }

        // 2. set current user
        Facebook facebook = Facebook.getInstance();
        facebook.setCurrentUser(user);

        // 3. upload meta & profile to DIM station
        userRegister.upload(user.identifier, user.getMeta(), user.getProfile());

        // 4. show main activity
        close();
    }
}
