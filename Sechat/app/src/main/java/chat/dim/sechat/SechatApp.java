package chat.dim.sechat;

import android.app.Application;

import java.util.HashMap;
import java.util.Map;

public class SechatApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, Object> options = new HashMap<>();
        options.put("Application", this);

        Client client = Client.getInstance();
        client.launch(options);
    }
}
