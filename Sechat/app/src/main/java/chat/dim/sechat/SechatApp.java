package chat.dim.sechat;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;

public class SechatApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, Object> options = new HashMap<>();
        options.put("Application", this);

        Client client = Client.getInstance();
        client.launch(options);
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
        ExternalStorage.setPath(path + File.separatorChar + "chat.dim.sechat");
    }
}
