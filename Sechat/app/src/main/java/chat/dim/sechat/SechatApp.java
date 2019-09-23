package chat.dim.sechat;

import android.app.Application;

import java.util.HashMap;
import java.util.Map;

import chat.dim.model.AccountDatabase;
import chat.dim.model.MessageProcessor;
import chat.dim.model.NetworkConfig;

public class SechatApp extends Application {

    Client client = Client.getInstance();
    AccountDatabase userDB = AccountDatabase.getInstance();
    MessageProcessor msgDB = MessageProcessor.getInstance();
    NetworkConfig networkConfig = NetworkConfig.getInstance();

    @Override
    public void onCreate() {
        super.onCreate();

        Map<String, Object> options = new HashMap<>();
        options.put("Application", this);

        client.launch(options);
    }
}
