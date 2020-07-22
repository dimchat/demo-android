package chat.dim.sechat.account;

import android.graphics.Bitmap;
import android.os.Bundle;

import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;
import chat.dim.sechat.R;
import chat.dim.ui.ImagePickerActivity;

public class UpdateAccountActivity extends ImagePickerActivity {

    private UpdateAccountFragment fragment;

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
            fragment.setAvatarImage(bitmap);
        }
    }
}
