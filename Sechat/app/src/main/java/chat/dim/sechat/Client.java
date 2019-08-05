package chat.dim.sechat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.common.Facebook;
import chat.dim.common.Server;
import chat.dim.common.Terminal;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.network.ServiceProvider;
import chat.dim.protocol.CommandContent;
import chat.dim.sechat.model.MessageProcessor;

public class Client extends Terminal {

    Facebook facebook = Facebook.getInstance();
    MessageProcessor msgDB = MessageProcessor.getInstance();

    private static final Client ourInstance = new Client();

    public static Client getInstance() {
        return ourInstance;
    }

    private Client() {
    }

    public String getDisplayName() {
        return "DIM!";
    }

    private void startServer(Map<String, Object> station, ServiceProvider sp) {

        ID identifier = ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");
        String ip = "134.175.87.98"; // from stationConfig["host"]
        Number port = 9394; // from stationConfig["port"]

        // prepare for launch star
        Map<String, Object> serverOptions = new HashMap<>();
        serverOptions.put("ID", identifier);
        serverOptions.put("host", ip);
        serverOptions.put("port", port);

        if (ip != null) {
            serverOptions.put("LongLinkAddress", "dim.chat");
            List<String> list = new ArrayList<>();
            list.add(ip);
            Map<String, Object> ipTable = new HashMap<>();
            ipTable.put("dim.chat", list);
            serverOptions.put("NewDNS", ipTable);
        }
        if (port != null) {
            serverOptions.put("LongLinkPort", port);
        }

        // TODO: config FTP server

        // connect server
        Server server = new Server(serverOptions);
        server.delegate = this;
        server.start(station);
        currentStation = server;

        // scan users
        User user = facebook.getUser(ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk"));
        addUser(user);
    }

    private void launchServiceProvider(Map<String, Object> spConfig) {
        ServiceProvider sp = null;
        // choose the fast station
        Map<String, Object> stationConfig = null; // from spConfig["stations"]

        startServer(stationConfig, sp);
    }

    //-------- AppDelegate

    public void launch(Map<String, Object> options) {

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
            facebook.saveMeta(meta, identifier);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // APNs?
        // Icon badge?

        //
        // launch server
        //

        // config Service Provider
        String spConfigFilePath = (String) options.get("ConfigFilePath");
        Map<String, Object> spConfig = null; // from spConfig file
        launchServiceProvider(spConfig);

        // TODO: scan users

        // TODO: notice("ProfileUpdated")
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

    static {
        // test
        Facebook facebook = Facebook.getInstance();

        Client client = Client.getInstance();

        if (false) {
            Map<String, Object> dictioanry = new HashMap<>();
            dictioanry.put("ID", "gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");
            dictioanry.put("host", "134.175.87.98");
            dictioanry.put("port", 9527);

            client.currentStation = new Server(dictioanry);

            ID identifier = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
            User user = facebook.getUser(identifier);
            client.setCurrentUser(user);
        }
    }
}
