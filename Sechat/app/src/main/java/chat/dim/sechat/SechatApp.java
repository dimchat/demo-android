package chat.dim.sechat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import chat.dim.common.KeyStore;
import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;
import chat.dim.io.Permissions;
import chat.dim.io.Resources;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.model.NetworkDatabase;
import chat.dim.sechat.push.jpush.JPushManager;
import chat.dim.sqlite.Database;
import chat.dim.sqlite.ans.AddressNameTable;
import chat.dim.sqlite.dim.LoginTable;
import chat.dim.sqlite.dim.MainDatabase;
import chat.dim.sqlite.dim.ProviderTable;
import chat.dim.sqlite.dkd.MessageDatabase;
import chat.dim.sqlite.dkd.MessageTable;
import chat.dim.sqlite.key.MsgKeyTable;
import chat.dim.sqlite.key.PrivateKeyTable;
import chat.dim.sqlite.mkm.ContactTable;
import chat.dim.sqlite.mkm.EntityDatabase;
import chat.dim.sqlite.mkm.GroupTable;
import chat.dim.sqlite.mkm.MetaTable;
import chat.dim.sqlite.mkm.ProfileTable;
import chat.dim.sqlite.mkm.UserTable;
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

    private void initDatabases() {
        Facebook facebook = Facebook.getInstance();
        Messenger messenger = Messenger.getInstance();

        // set context for databases
        Database.setContext(this);
        MainDatabase.setContext(this);
        EntityDatabase.facebook = facebook;
        MessageDatabase.messenger = messenger;

        // tables
        NetworkDatabase netDB = NetworkDatabase.getInstance();
        netDB.providerTable = ProviderTable.getInstance();

        facebook.privateTable = PrivateKeyTable.getInstance();
        facebook.metaTable = MetaTable.getInstance();
        facebook.profileTable = ProfileTable.getInstance();
        facebook.userTable = UserTable.getInstance();
        facebook.contactTable = ContactTable.getInstance();
        facebook.groupTable = GroupTable.getInstance();
        facebook.ansTable = AddressNameTable.getInstance();

        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        msgDB.messageTable = MessageTable.getInstance();

        KeyStore keyStore = KeyStore.getInstance();
        keyStore.keyTable = MsgKeyTable.getInstance();

        LoginCommandProcessor.loginTable = LoginTable.getInstance();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initDatabases();

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
        ExternalStorage.setRoot(path);
    }
}
