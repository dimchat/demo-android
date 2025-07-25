package chat.dim.sechat;

import android.app.Activity;
import android.graphics.Bitmap;

import java.util.HashMap;
import java.util.Map;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.crypto.KeyStore;
import chat.dim.format.Base64;
import chat.dim.format.DataCoder;
import chat.dim.io.Permissions;
import chat.dim.io.Resources;
import chat.dim.log.DefaultLogger;
import chat.dim.log.Log;
import chat.dim.log.LogCaller;
import chat.dim.log.LogPrinter;
import chat.dim.log.Logger;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.NetworkDatabase;
import chat.dim.sqlite.Database;
import chat.dim.sqlite.ans.AddressNameTable;
import chat.dim.sqlite.dim.LoginTable;
import chat.dim.sqlite.dim.ProviderTable;
import chat.dim.sqlite.dkd.MessageTable;
import chat.dim.sqlite.key.MsgKeyTable;
import chat.dim.sqlite.key.PrivateKeyTable;
import chat.dim.sqlite.mkm.ContactTable;
import chat.dim.sqlite.mkm.DocumentTable;
import chat.dim.sqlite.mkm.EntityDatabase;
import chat.dim.sqlite.mkm.GroupTable;
import chat.dim.sqlite.mkm.MetaTable;
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
        // set context for databases
        Database.context = this;

        SharedFacebook.ansTable = AddressNameTable.getInstance();
        KeyStore keyStore = KeyStore.getInstance();
        keyStore.keyTable = MsgKeyTable.getInstance();

        GlobalVariable shared = GlobalVariable.getInstance();

        EntityDatabase.facebook = shared.facebook;
        //MessageDatabase.messenger = messenger;

        // tables
        NetworkDatabase netDB = NetworkDatabase.getInstance();
        netDB.providerTable = ProviderTable.getInstance();

        shared.database.privateKeyTable = PrivateKeyTable.getInstance();
        shared.database.metaTable = MetaTable.getInstance();
        shared.database.documentTable = DocumentTable.getInstance();
        shared.database.userTable = UserTable.getInstance();
        shared.database.contactTable = ContactTable.getInstance();
        shared.database.groupTable = GroupTable.getInstance();

        shared.database.msgKeyTable = keyStore;
        shared.database.loginTable = LoginTable.getInstance();
        shared.database.providerTable = ProviderTable.getInstance();

        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        msgDB.messageTable = MessageTable.getInstance();

        shared.terminal = new Client(shared.facebook, shared.database);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        initDatabases();

//        //初始化推送
//        JPushManager.getInstance().init(this, BuildConfig.DEBUG);
    }

    @Override
    protected void onEnterForeground(Activity activity) {
        Client client = Client.getInstance();
        if (client != null) {
            client.enterForeground();
        }
    }

    @Override
    protected void onEnterBackground(Activity activity) {
        Client client = Client.getInstance();
        if (client != null) {
            client.enterBackground();
        }
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

        Log.level = Log.DEBUG;
        //Log.showCaller = false;
        Log.logger = new DefaultLogger(new LogPrinter() {
            @Override
            protected void println(String tag, LogCaller caller, String msg) {
                String filename;
                if (caller == null) {
                    filename = null;
                } else {
                    filename = caller.filename.split("\\.")[0];
                }
                switch (tag) {
                    case Logger.DEBUG_TAG:
                        android.util.Log.d(filename == null ? "Debug" : filename, msg);
                        break;
                    case Logger.INFO_TAG:
                        android.util.Log.i(filename == null ? "Log" : filename, msg);
                        break;
                    case Logger.WARNING_TAG:
                        android.util.Log.w(filename == null ? "Warning" : filename, msg);
                        break;
                    case Logger.ERROR_TAG:
                        android.util.Log.e(filename == null ? "Error" : filename, msg);
                        break;
                }
            }
        });

        // prepare plugins
        GlobalVariable shared = GlobalVariable.getInstance();
        assert shared != null;

        // android.Base64
        Base64.coder = new DataCoder() {
            @Override
            public String encode(byte[] data) {
                return android.util.Base64.encodeToString(data, android.util.Base64.DEFAULT);
            }

            @Override
            public byte[] decode(String string) {
                return android.util.Base64.decode(string, android.util.Base64.DEFAULT);
            }
        };
    }
}
