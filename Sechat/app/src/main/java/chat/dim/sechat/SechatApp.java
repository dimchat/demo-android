package chat.dim.sechat;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import chat.dim.filesys.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;
import chat.dim.io.Permissions;
import chat.dim.io.Resources;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.sqlite.ContactTable;
import chat.dim.sqlite.EntityDatabase;
import chat.dim.sqlite.GroupTable;
import chat.dim.sqlite.MessageDatabase;
import chat.dim.sqlite.MessageTable;
import chat.dim.ui.Application;

public class SechatApp extends Application {

    private static SechatApp ourInstance = null;
    public static SechatApp getInstance() { return ourInstance; }
    public SechatApp() {
        super();
        ourInstance = this;

        // databases
        EntityDatabase.setContext(this);
        Facebook facebook = Facebook.getInstance();
        facebook.groupTable = GroupTable.getInstance();
        facebook.contactTable = ContactTable.getInstance();

        MessageDatabase.setContext(this);
        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        msgDB.messageTable = MessageTable.getInstance();
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
        ExternalStorage.setRoot(path + File.separator + "chat.dim.sechat");
    }
}
