package chat.dim.sechat.account;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.io.IOException;

import chat.dim.database.Database;
import chat.dim.sechat.R;
import chat.dim.ui.image.ImagePickerActivity;

public class UpdateAccountActivity extends ImagePickerActivity {

    private UpdateAccountFragment fragment;

    public UpdateAccountActivity() {
        super();
        setCrop(true);
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

    void exportAccount(AccountViewModel viewModel) {
        ExportFragment fragment = ExportFragment.newInstance(viewModel);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.container, fragment);
        transaction.commit();
    }

    //
    //  ImagePickerActivity
    //

    @Override
    protected String getTemporaryDirectory() throws IOException {
        return Database.getTemporaryDirectory();
    }

    @Override
    protected void fetchImage(Bitmap bitmap) {
        if (bitmap != null) {
            fragment.setAvatarImage(bitmap);
        }
    }
}
