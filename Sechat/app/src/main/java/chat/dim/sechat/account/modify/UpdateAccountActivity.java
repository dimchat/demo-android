package chat.dim.sechat.account.modify;

import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.ActionBar;
import android.view.MenuItem;

import chat.dim.filesys.LocalCache;
import chat.dim.sechat.R;
import chat.dim.sechat.account.AccountViewModel;
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

    public void exportAccount(AccountViewModel viewModel) {
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
    protected String getTemporaryDirectory() {
        LocalCache cache = LocalCache.getInstance();
        return cache.getTemporaryDirectory();
    }

    @Override
    protected void fetchImage(Bitmap bitmap) {
        if (bitmap != null) {
            fragment.setAvatarImage(bitmap);
        }
    }
}
