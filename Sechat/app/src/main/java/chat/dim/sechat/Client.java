package chat.dim.sechat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.client.Facebook;
import chat.dim.client.Server;
import chat.dim.client.Terminal;
import chat.dim.core.Barrack;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.network.ServiceProvider;
import chat.dim.protocol.command.BroadcastCommand;
import chat.dim.sechat.model.MessageProcessor;

public class Client extends Terminal {
    private static final Client ourInstance = new Client();

    public static Client getInstance() {
        return ourInstance;
    }

    private Client() {
    }

    public String getDisplayName() {
        return "DIM!";
    }

    //-------- AppDelegate

    public void launch(Map<String, Object> options) {

        // APNs?
        // Icon badge?

        //
        // launch server
        //
//        Facebook facebook = Facebook.getInstance();

        // config Service Provider
        String spConfigFilePath = (String) options.get("ConfigFilePath");
        Map<String, Object> spConfig = null; // from spConfig file
        ServiceProvider sp = null;
        // choose the fast station
        Map<String, Object> stationConfig = null; // from spConfig["stations"]
        ID identifier = ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");
        Meta meta = null;
//        facebook.saveMeta(meta, identifier);

        // prepare for launch star
        Map<String, Object> serverOptions = new HashMap<>();
        String ip = "134.175.87.98"; // from stationConfig["host"]
        if (ip != null) {
            serverOptions.put("LongLinkAddress", "dim.chat");
            List<String> list = new ArrayList<>();
            list.add(ip);
            Map<String, Object> ipTable = new HashMap<>();
            ipTable.put("dim.chat", list);
            serverOptions.put("NewDNS", ipTable);
        }
        Number port = 9394; // from stationConfig["port"]
        if (port != null) {
            serverOptions.put("LongLinkPort", port);
        }

        // TODO: config FTP server

        // connect server
        Server server = new Server(stationConfig);
        server.delegate = this;
        server.start(serverOptions);
        currentStation = server;

        MessageProcessor.getInstance();
//        facebook.addStation(identifier, sp);

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
        BroadcastCommand cmd = new BroadcastCommand("report");
        cmd.put("state", "background");
        sendCommand(cmd);

        currentStation.pause();
    }

    public void enterForeground() {
        currentStation.resume();

        // clear icon badge

        // report client state
        BroadcastCommand cmd = new BroadcastCommand("report");
        cmd.put("state", "foreground");
        sendCommand(cmd);
    }

    static {
        // test
        Barrack barrack = Facebook.getInstance();

        Client client = Client.getInstance();

        {
            Map<String, Object> dictioanry = new HashMap<>();
            dictioanry.put("ID", "gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");
            dictioanry.put("host", "134.175.87.98");
            dictioanry.put("port", 9527);

            client.currentStation = new Server(dictioanry);
        }
        {
            ID identifier = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");
            User user = barrack.getUser(identifier);
            client.setCurrentUser(user);
        }
    }
}
