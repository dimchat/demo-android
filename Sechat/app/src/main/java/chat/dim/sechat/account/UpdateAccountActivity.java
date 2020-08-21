package chat.dim.sechat.account;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;

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

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            fragment = UpdateAccountFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commitNow();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
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
