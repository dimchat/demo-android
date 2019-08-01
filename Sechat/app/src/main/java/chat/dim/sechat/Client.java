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
        Facebook facebook = Facebook.getInstance();
        MessageProcessor msgDB = MessageProcessor.getInstance();

        // config Service Provider
        String spConfigFilePath = (String) options.get("ConfigFilePath");
        Map<String, Object> spConfig = null; // from spConfig file
        ServiceProvider sp = null;
        // choose the fast station
        Map<String, Object> stationConfig = null; // from spConfig["stations"]
        ID identifier = ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");
        Meta meta = null;
//        facebook.saveMeta(meta, identifier);

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
        server.start(options);
        currentStation = server;

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
