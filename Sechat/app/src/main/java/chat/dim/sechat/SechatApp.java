package chat.dim.sechat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;
import chat.dim.io.Permissions;
import chat.dim.io.Resources;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.model.NetworkDatabase;
import chat.dim.sechat.push.jpush.JPushManager;
import chat.dim.sqlite.ANSDatabase;
import chat.dim.sqlite.ANSTable;
import chat.dim.sqlite.ContactTable;
import chat.dim.sqlite.EntityDatabase;
import chat.dim.sqlite.GroupTable;
import chat.dim.sqlite.LoginTable;
import chat.dim.sqlite.MessageDatabase;
import chat.dim.sqlite.MessageTable;
import chat.dim.sqlite.ProviderDatabase;
import chat.dim.sqlite.ProviderTable;
import chat.dim.sqlite.UserTable;
import chat.dim.ui.Application;

public class SechatApp extends Application {

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
        ProviderDatabase.setContext(this);
        ANSDatabase.setContext(this);
        EntityDatabase.setContext(this);
        MessageDatabase.setContext(this);

        NetworkDatabase netDB = NetworkDatabase.getInstance();
        netDB.providerTable = ProviderTable.getInstance();

        Facebook facebook = Facebook.getInstance();
        facebook.userTable = UserTable.getInstance();
        facebook.contactTable = ContactTable.getInstance();
        facebook.groupTable = GroupTable.getInstance();
        facebook.ansTable = ANSTable.getInstance();

        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        msgDB.messageTable = MessageTable.getInstance();

        LoginCommandProcessor.dataHandler = LoginTable.getInstance();

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
