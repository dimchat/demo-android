package chat.dim.sechat;

import android.app.Activity;
import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;
import chat.dim.ui.Permissions;
import chat.dim.ui.Resources;
import chat.dim.ui.image.Images;

public class SechatApp extends Application {

    private static SechatApp ourInstance = null;
    public static SechatApp getInstance() { return ourInstance; }
    public SechatApp() {
        super();
        ourInstance = this;
    }

    public static boolean launch(Application app, Activity activity) {
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
            icon = getBitmapFromMipmap(R.mipmap.ic_launcher_foreground);
        }
        return icon;
    }

    public Uri getUriFromMipmap(int resId) {
        return Resources.getUriFromMipmap(this, resId);
    }
    public Bitmap getBitmapFromMipmap(int resId) {
        Uri uri = getUriFromMipmap(resId);
        try {
            return Images.bitmapFormUri(uri, getContentResolver());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

        String path = Environment.getExternalStorageDirectory().getPath();
        ExternalStorage.root = path + ExternalStorage.separator + "chat.dim.sechat";
    }
}
