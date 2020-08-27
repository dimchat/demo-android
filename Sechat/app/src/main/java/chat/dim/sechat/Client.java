package chat.dim.sechat;

import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.network.Terminal;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.sechat.model.GroupViewModel;

public final class Client extends Terminal implements Observer {

    private static final Client ourInstance = new Client();
    public static Client getInstance() { return ourInstance; }
    private Client() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MembersUpdated);
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MembersUpdated)) {
            ID group = (ID) info.get("group");
            GroupViewModel.refreshLogo(group);
        }
    }

    private PackageInfo getPackageInfo(ContextWrapper app) {
        PackageManager packageManager = app.getPackageManager();
        try {
            return packageManager.getPackageInfo(app.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public String getDisplayName() {
        SechatApp app = SechatApp.getInstance();
        PackageInfo packInfo = getPackageInfo(app);
        if (packInfo == null) {
            return null;
        }
        int labelRes = packInfo.applicationInfo.labelRes;
        return app.getResources().getString(labelRes);
    }

    @Override
    public String getVersionName() {
        SechatApp app = SechatApp.getInstance();
        PackageInfo packInfo = getPackageInfo(app);
        if (packInfo == null) {
            return null;
        }
        return packInfo.versionName;
    }

    @Override
    public String getSystemVersion() {
        return android.os.Build.VERSION.RELEASE;
    }

    @Override
    public String getSystemModel() {
        return android.os.Build.MODEL;
    }

    @Override
    public String getSystemDevice() {
        return android.os.Build.DEVICE;
    }

    @Override
    public String getDeviceBrand() {
        return android.os.Build.BRAND;
    }

    @Override
    public String getDeviceBoard() {
        return android.os.Build.BOARD;
    }

    @Override
    public String getDeviceManufacturer() {
        return android.os.Build.MANUFACTURER;
    }

    public void startChat(ID entity) {
        Map<String, Object> info = new HashMap<>();
        info.put("ID", entity);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.StartChat, this, info);
    }
}
