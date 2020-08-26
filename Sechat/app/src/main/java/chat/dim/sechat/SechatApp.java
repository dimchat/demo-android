package chat.dim.sechat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;
import chat.dim.io.Permissions;
import chat.dim.io.Resources;
import chat.dim.sechat.push.jpush.JPushManager;
import chat.dim.sqlite.Database;
import chat.dim.ui.Application;

public final class SechatApp extends Application {

    private static SechatApp ourInstance = null;

    public static SechatApp getInstance() {
        return ourInstance;
    }

    public SechatApp() {
        super();
        ourInstance = this;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // databases
        Database.setContext(this);

        //初始化推送
        JPushManager.getInstance().init(this, BuildConfig.DEBUG);
    }

    @Override
    protected void onEnterForeground(Activity activity) {
        Client client = Client.getInstance();
        client.enterForeground();
    }

    @Override
    protected void onEnterBackground(Activity activity) {
        Client client = Client.getInstance();
        client.enterBackground();
    }

    public static boolean launch(android.app.Application app, Activity activity) {
        if (!Permissions.canWriteExternalStorage(activity)) {
            Permissions.requestExternalStoragePermissions(activity);
            return false;
        }

        Map<String, Object> options = new HashMap<>();
        options.put("Application", app);

        Client client = Client.getInstance();
        client.launch(options);
        return true;
    }

    private Bitmap icon = null;

    public Bitmap getIcon() {
        if (icon == null) {
            icon = Resources.getBitmapFromMipmap(this, R.mipmap.ic_launcher_foreground);
        }
        return icon;
    }

    static {
        // android.Base64
        Base64.coder = new BaseCoder() {
            @Override
            public String encode(byte[] data) {
                return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            }

            @Override
            public byte[] decode(String string) {
                return android.util.Base64.decode(string, android.util.Base64.DEFAULT);
            }
        };

        String path = Environment.getExternalStorageDirectory().getAbsolutePath();
        path += File.separator + "chat.dim.sechat";
        try {
            ExternalStorage.mkdirs(path);
        } catch (IOException e) {
            e.printStackTrace();
        }
        ExternalStorage.setRoot(path);
    }
}
