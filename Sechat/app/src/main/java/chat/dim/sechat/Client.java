package chat.dim.sechat;

import android.content.ContextWrapper;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ClientMessenger;
import chat.dim.CommonFacebook;
import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.SharedMessenger;
import chat.dim.SharedPacker;
import chat.dim.SharedProcessor;
import chat.dim.Terminal;
import chat.dim.core.Packer;
import chat.dim.core.Processor;
import chat.dim.dbi.SessionDBI;
import chat.dim.mkm.User;
import chat.dim.model.NetworkDatabase;
import chat.dim.network.ClientSession;
import chat.dim.network.SessionState;
import chat.dim.network.StateMachine;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sqlite.dim.ProviderTable;
import chat.dim.threading.BackgroundThreads;
import chat.dim.utils.Log;

public final class Client extends Terminal implements Observer {

    public Client(SharedFacebook facebook, SessionDBI database) {
        super(facebook, database);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MembersUpdated);
    }

    public static Client getInstance() {
        GlobalVariable shared = GlobalVariable.getInstance();
        return (Client) shared.terminal;
    }

    @Override
    public void finalize() throws Throwable {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.MembersUpdated);
        super.finalize();
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

    @Override
    protected Packer createPacker(CommonFacebook facebook, ClientMessenger messenger) {
        return new SharedPacker(facebook, messenger);
    }

    @Override
    protected Processor createProcessor(CommonFacebook facebook, ClientMessenger messenger) {
        return new SharedProcessor(facebook, messenger);
    }

    @Override
    protected ClientMessenger createMessenger(ClientSession session, CommonFacebook facebook) {
        GlobalVariable shared = GlobalVariable.getInstance();
        shared.messenger = new SharedMessenger(session, facebook, shared.mdb);
        return shared.messenger;
    }

    public void startChat(ID entity) {
        Map<String, Object> info = new HashMap<>();
        info.put("ID", entity);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.StartChat, this, info);
    }

    //--------

    private void startServer(ProviderTable.StationInfo stationInfo) {
        ID identifier = stationInfo.identifier;
        String name = stationInfo.name;
        String host = stationInfo.host;
        int port = stationInfo.port;

        Map<String, Object> options = new HashMap<>();
        options.put("ID", identifier);
        options.put("host", host);
        options.put("port", port);

        if (host != null) {
            options.put("LongLinkAddress", "dim.chat");
            List<String> list = new ArrayList<>();
            list.add(stationInfo.host);
            Map<String, Object> ipTable = new HashMap<>();
            ipTable.put("dim.chat", list);
            options.put("NewDNS", ipTable);
        }
        if (port != 0) {
            options.put("LongLinkPort", stationInfo.port);
        }

        // TODO: config FTP server

        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;

        // FIXME: debug
        //host = "192.168.31.91";
        host = "106.52.25.169";
        port = 9394;

        // connect server
        connect(host, port);

        // get user from database and login
        User user = facebook.getCurrentUser();
        if (user != null) {
            login(user.getIdentifier());
        }
    }

    private void startServer() {
        NetworkDatabase database = NetworkDatabase.getInstance();
        List<ProviderTable.ProviderInfo> providers = database.allProviders();
        if (providers.size() > 0) {
            // choose the default sp
            ProviderTable.ProviderInfo sp = providers.get(0);
            List<ProviderTable.StationInfo> stations = database.allStations(sp.identifier);
            if (stations != null && stations.size() > 0) {
                // choose the default station
                ProviderTable.StationInfo srv = stations.get(0);
                startServer(srv);
            }
        }
    }

    //-------- AppDelegate

    public void launch(Map<String, Object> options) {
        Log.info("launch client: " + options);


        //
        //  launch server in background
        //
        BackgroundThreads.rush(this::startServer);


        // TODO: notice("DocumentUpdated")

        // APNs?
        // Icon badge?
    }


    //
    //  FSM Delegate
    //

    @Override
    public void enterState(SessionState next, StateMachine ctx) {
        super.enterState(next, ctx);
        // called after state changed
    }

    @Override
    public void exitState(SessionState previous, StateMachine ctx) {
        super.exitState(previous, ctx);
        // called after state changed
        Log.info("state changed: " + previous + " => " + ctx.getCurrentState());
        SessionState current = ctx.getCurrentState();
        Log.info("server state changed: " + previous + " -> " + current);
        if (current == null) {
            return;
        }
        Map<String, Object> info = new HashMap<>();
        info.put("state", current.name);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.ServerStateChanged, this, info);
    }

    @Override
    public void pauseState(SessionState current, StateMachine ctx) {
        super.pauseState(current, ctx);
    }

    @Override
    public void resumeState(SessionState current, StateMachine ctx) {
        super.resumeState(current, ctx);
        // TODO: clear session key for re-login?
    }
}
