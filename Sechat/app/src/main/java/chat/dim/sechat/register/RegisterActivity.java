package chat.dim.sechat.register;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import chat.dim.filesys.LocalCache;
import chat.dim.io.Permissions;
import chat.dim.sechat.MainActivity;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
//import chat.dim.sechat.push.jpush.JPushManager;
import chat.dim.ui.image.ImagePickerActivity;

public class RegisterActivity extends ImagePickerActivity {

    RegisterFragment registerFragment = null;
    ImportFragment importFragment = null;

    public RegisterActivity() {
        super();
        setCrop(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register_activity);
//        JPushManager.getInstance().setAlias("");  //注册页面停止推送
        setTitle(R.string.register);

        MainActivity.initLocalStorage(this);

        tryLaunch();

        if (savedInstanceState == null) {
            registerFragment = RegisterFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, registerFragment)
                    .commitNow();
        }
    }

    boolean tryLaunch() {
        try {
            return SechatApp.launch(getApplication(), this);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    void close() {
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK|Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClass(getApplicationContext(), MainActivity.class);
        startActivity(intent);
    }

    void showImportPage() {
        if (!Permissions.canWriteExternalStorage(this)) {
            Permissions.requestExternalStoragePermissions(this);
            return;
        }
        if (importFragment == null) {
            importFragment = ImportFragment.newInstance();
        }
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.add(R.id.container, importFragment);
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
        registerFragment.fetchImage(bitmap);
    }
}
