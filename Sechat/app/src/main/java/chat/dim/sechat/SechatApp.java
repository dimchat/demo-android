package chat.dim.sechat;

import android.app.Activity;
import android.app.Application;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;

import java.util.HashMap;
import java.util.Map;

import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;

public class SechatApp extends Application {

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String READ_EXTERNAL_STORAGE = "android.permission.READ_EXTERNAL_STORAGE";
    private static String WRITE_EXTERNAL_STORAGE = "android.permission.WRITE_EXTERNAL_STORAGE";
    private static String[] PERMISSIONS_STORAGE = {
            READ_EXTERNAL_STORAGE,
            WRITE_EXTERNAL_STORAGE
    };

    private static boolean verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, WRITE_EXTERNAL_STORAGE);
        if (permission == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE, REQUEST_EXTERNAL_STORAGE);
        return false;
    }

    public static boolean launch(Application app, Activity activity) {
        if (!verifyStoragePermissions(activity)) {
            return false;
        }

        Map<String, Object> options = new HashMap<>();
        options.put("Application", app);

        Client client = Client.getInstance();
        client.launch(options);
        return true;
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
