package chat.dim.sechat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.client.Facebook;
import chat.dim.database.SocialNetworkDatabase;
import chat.dim.database.StationTable;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.ID;
import chat.dim.network.Connection;
import chat.dim.network.Server;
import chat.dim.network.ServiceProvider;
import chat.dim.network.Terminal;
import chat.dim.protocol.Command;

public class Client extends Terminal {
    private static final Client ourInstance = new Client();
    public static Client getInstance() { return ourInstance; }
    private Client() {
        super();
        SocialNetworkDatabase.getInstance().reloadData();
    }

    private final Facebook facebook = Facebook.getInstance();

    public String getDisplayName() {
        return "DIM!";
    }

    private LocalUser getUser(Object identifier) {
        Facebook facebook = Facebook.getInstance();
        return (LocalUser) facebook.getUser(facebook.getID(identifier));
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
        connection = new Connection(server);
        server.start(station);
    }

    @SuppressWarnings("unchecked")
    private void launchServiceProvider(Map<String, Object> spConfig) {
        ID spID = facebook.getID(spConfig.get("ID"));
        ServiceProvider sp = new ServiceProvider(spID);

        List<Map> stations = (List<Map>) spConfig.get("stations");
        if (stations == null) {
            stations = StationTable.allStations(spID);
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
            spConfig = StationTable.getProviderConfig(ID.ANYONE);
        }
        launchServiceProvider(spConfig);

        // TODO: notice("ProfileUpdated")

        // APNs?
        // Icon badge?
    }

    public void terminate() {
        if (connection != null) {
            connection.server.end();
        }
    }


    public void enterBackground() {
        if (connection != null) {
            // report client state
            Command cmd = new Command("broadcast");
            cmd.put("title", "report");
            cmd.put("state", "background");
            connection.sendCommand(cmd);
            // pause the server
            connection.server.pause();
        }
    }

    public void enterForeground() {
        if (connection != null) {
            // resume the server
            connection.server.resume();

            // clear icon badge

            // report client state
            Command cmd = new Command("broadcast");
            cmd.put("title", "report");
            cmd.put("state", "foreground");
            connection.sendCommand(cmd);
        }
    }
}
