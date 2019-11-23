package chat.dim.sechat;

import android.os.Environment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.database.ExternalStorage;
import chat.dim.format.Base64;
import chat.dim.format.BaseCoder;
import chat.dim.mkm.User;
import chat.dim.mkm.ID;
import chat.dim.model.Facebook;
import chat.dim.model.NetworkDatabase;
import chat.dim.network.Server;
import chat.dim.network.ServiceProvider;
import chat.dim.network.Terminal;
import chat.dim.protocol.Command;
import chat.dim.stargate.simplegate.Fence;

public class Client extends Terminal {

    static {
        // mkm.Base64
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
        ExternalStorage.root = path + "/chat.dim.sechat";
    }

    private static final Client ourInstance = new Client();
    public static Client getInstance() { return ourInstance; }
    private Client() {
        super();
    }

    public String getDisplayName() {
        return "DIM!";
    }

    private User getUser(Object identifier) {
        Facebook facebook = Facebook.getInstance();
        return facebook.getUser(facebook.getID(identifier));
    }

    private void startServer(Map<String, Object> station, ServiceProvider sp) {

        ID identifier = ID.getInstance(station.get("ID"));
        String host = (String) station.get("host");
        int port = (int) station.get("port");

        // prepare for launch star
        if (host != null) {
            station.put("LongLinkAddress", "dim.chat");
            List<String> list = new ArrayList<>();
            list.add(host);
            Map<String, Object> ipTable = new HashMap<>();
            ipTable.put("dim.chat", list);
            station.put("NewDNS", ipTable);
        }
        if (port != 0) {
            station.put("LongLinkPort", port);
        }

        // TODO: config FTP server

        // connect server
        Server server = new Server(station);
        server.delegate = this;
        server.star = new Fence(server);
        server.start(station);
        setCurrentServer(server);

        // get user from database and login
        login(null);
    }

    @SuppressWarnings("unchecked")
    private void launchServiceProvider(Map<String, Object> spConfig) {
        Facebook facebook = Facebook.getInstance();
        ID spID = facebook.getID(spConfig.get("ID"));
        ServiceProvider sp = new ServiceProvider(spID);

        List<Map<String, Object>> stations = (List) spConfig.get("stations");
        if (stations == null) {
            stations = NetworkDatabase.getInstance().allStations(spID);
            assert stations != null;
        }

        // choose the fast station
        Map<String, Object> neighbor = new HashMap<>(stations.get(0));

        startServer(neighbor, sp);
    }

    //-------- AppDelegate

    @SuppressWarnings("unchecked")
    public void launch(Map<String, Object> options) {

        //
        // launch server
        //
        Map<String, Object> spConfig = (Map<String, Object>) options.get("SP");
        if (spConfig == null) {
            spConfig = NetworkDatabase.getInstance().getProviderConfig(ID.ANYONE);
        }
        launchServiceProvider(spConfig);

        // TODO: notice("ProfileUpdated")

        // APNs?
        // Icon badge?
    }

    public void terminate() {
        Server server = getCurrentServer();
        if (server != null) {
            server.end();
        }
    }


    public void enterBackground() {
        Server server = getCurrentServer();
        if (server != null) {
            // report client state
            Command cmd = new Command("broadcast");
            cmd.put("title", "report");
            cmd.put("state", "background");
            sendCommand(cmd);
            // pause the server
            server.pause();
        }
    }

    public void enterForeground() {
        Server server = getCurrentServer();
        if (server != null) {
            // resume the server
            server.resume();

            // clear icon badge

            // report client state
            Command cmd = new Command("broadcast");
            cmd.put("title", "report");
            cmd.put("state", "foreground");
            sendCommand(cmd);
        }
    }

    static {
        Facebook facebook = Facebook.getInstance();

        // FIXME: test data
        ID hulk = facebook.getID("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");
        ID moki = facebook.getID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");

        User user = facebook.getUser(hulk);
        facebook.setCurrentUser(user);
        facebook.addContact(moki, user.identifier);
    }
}
