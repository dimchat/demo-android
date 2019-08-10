package chat.dim.sechat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.client.Facebook;
import chat.dim.database.SocialNetworkDatabase;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.network.Server;
import chat.dim.network.ServiceProvider;
import chat.dim.network.Terminal;
import chat.dim.protocol.CommandContent;
import chat.dim.protocol.command.HandshakeCommand;

public class Client extends Terminal {
    private static final Client ourInstance = new Client();
    public static Client getInstance() { return ourInstance; }
    private Client() {
        super();
        ID user = getLastUser();
        SocialNetworkDatabase.getInstance().reloadData(user);
    }

    private final Facebook facebook = Facebook.getInstance();

    public String getDisplayName() {
        return "DIM!";
    }

    private ID getLastUser() {
        Facebook facebook = Facebook.getInstance();
        return facebook.getID("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
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
        server.start(station);
        currentStation = server;

        // scan users
        ID last = getLastUser();
        if (last != null) {
            addUser(getUser(last));
        }

        // FIXME: handshake after connected
        server.handshake(null);
    }

    @SuppressWarnings("unchecked")
    private void launchServiceProvider(Map<String, Object> spConfig) {
        ServiceProvider sp = null;

        List stations = (List) spConfig.get("stations");
        assert stations != null;

        // choose the fast station
        Map<String, Object> stationConfig = (Map<String, Object>) stations.get(0);

        startServer(stationConfig, sp);
    }

    //-------- AppDelegate

    public void launch(Map<String, Object> options) {

        // station IP
        String host = "127.0.0.1";
        //String host = "134.175.87.98";

        // station Port
        int port = 9394;

        // station ID
        ID identifier = ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");

        // station meta
        Map<String, Object> keyDict = new HashMap<>();
        keyDict.put("algorithm", "RSA");
        keyDict.put("data", "-----BEGIN PUBLIC KEY-----\n" +
                "MIGJAoGBAMRPt+8u6lQFRzoibAl6MKiXJuqtT5jo/CIIqFuUZvBWpu9qkPYDWGMS7gS0vqabe/uF\n" +
                "yCw7cN/ff9KFG5roZb38lBUMt93Oct+5ODzDlSD53Kwl9uuYHUbuduoNgTJdBc1FalCwsdhIP+KF\n" +
                "pIpY2c65XRzuJ4kTNACn74753dZRAgMBAAE\n" +
                "-----END PUBLIC KEY-----\n");
        Map<String, Object> metaDict = new HashMap<>();
        metaDict.put("version", 1);
        metaDict.put("seed", "gsp-s001");
        metaDict.put("key", keyDict);
        metaDict.put("fingerprint", "R+Bv3RlVi8pNuVWDJ8uEp+N3l+B04ftlaNFxo7u8+V6eSQsQJNv7tfQNFdC633UpXDw3zZHvQNnkUBwthaCJTbEmy2CYqMSx/BLuaS7spkSZJJAT7++xqif+pRjdw9yM/aPufKHS4PAvGec21PsUpQzOV5TQFyF5CDEDVLC8HVY=");
        try {
            Meta meta = Meta.getInstance(metaDict);
            Facebook.getInstance().saveMeta(meta, identifier);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // station config
        Map<String, Object> srvConfig = new HashMap<>();
        srvConfig.put("ID", identifier);
        srvConfig.put("meta", metaDict);
        srvConfig.put("host", host);
        srvConfig.put("port", port);

        // station list
        List<Map> stations = new ArrayList<>();
        stations.add(srvConfig);

        // SP config
        String spConfigFilePath = (String) options.get("ConfigFilePath");
        Map<String, Object> spConfig = new HashMap<>(); // from spConfig file
        spConfig.put("stations", stations);

        //
        // launch server
        //
        launchServiceProvider(spConfig);

        // TODO: scan users
        identifier = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
        User user = facebook.getUser(identifier);
        setCurrentUser(user);

        // TODO: notice("ProfileUpdated")

        // APNs?
        // Icon badge?
    }

    public void terminate() {
        if (currentStation != null) {
            currentStation.end();
        }
    }


    public void enterBackground() {
        // report client state
        CommandContent cmd = new CommandContent("broadcast");
        cmd.put("title", "report");
        cmd.put("state", "background");
        sendCommand(cmd);

        currentStation.pause();
    }

    public void enterForeground() {
        currentStation.resume();

        // clear icon badge

        // report client state
        CommandContent cmd = new CommandContent("broadcast");
        cmd.put("title", "report");
        cmd.put("state", "foreground");
        sendCommand(cmd);
    }
}
