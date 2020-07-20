package chat.dim.sechat.account;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

import chat.dim.filesys.ExternalStorage;
import chat.dim.sechat.R;
import chat.dim.ui.ImagePicker;
import chat.dim.ui.Resources;

public class UpdateAccountActivity extends AppCompatActivity {

    final ImagePicker imagePicker;
    private UpdateAccountFragment fragment;

    public UpdateAccountActivity() {
        super();
        imagePicker = new ImagePicker(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_account_activity);

        if (savedInstanceState == null) {
            fragment = UpdateAccountFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commitNow();
        }
    }

    private void fetchAvatar(Intent data) {
        Bitmap avatarImage = imagePicker.getBitmap(data);
        if (avatarImage != null) {
            fragment.setAvatarImage(avatarImage);
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
}
