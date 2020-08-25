/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.database.ProviderTable;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.model.NetworkDatabase;
import chat.dim.protocol.Command;
import chat.dim.protocol.LoginCommand;

public class Terminal implements StationDelegate {

    protected Facebook facebook = Facebook.getInstance();
    protected Messenger messenger = Messenger.getInstance();

    private Server currentServer = null;

    public Terminal() {
        super();
    }

    /**
     *  format: "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) DIMCoreKit/1.0 (Terminal, like WeChat) DIM-by-GSP/1.0.1"
     */
    public String getUserAgent() {
        String model = "Android";
        String sysName = "HMS";
        String sysVersion = "4.0";
        String lang = "zh-CN";

        return String.format("DIMP/1.0 (%s; U; %s %s; %s)" +
                        " DIMCoreKit/1.0 (Terminal, like WeChat) DIM-by-GSP/1.0.1",
                model, sysName, sysVersion, lang);
    }

    public String getLanguage() {
        return "zh-CN";
    }

    protected Server getCurrentServer() {
        return currentServer;
    }

    protected void setCurrentServer(Server server) {
        server.delegate = this;
        messenger.server = server;
        messenger.setContext("server", server);
        currentServer = server;
    }

    public User getCurrentUser() {
        return currentServer == null ? null : currentServer.getCurrentUser();
    }

    public void setCurrentUser(User user) {
        if (currentServer != null) {
            currentServer.setCurrentUser(user);
        }
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

        // connect server
        Server server = getCurrentServer();
        if (server == null || server.getPort() != port || !server.getHost().equals(host)) {
            server = new Server(identifier, host, port, name);
            server.delegate = this;
            server.start(options);
            setCurrentServer(server);
        }

        // get user from database and login
        messenger.login(null);
    }

    //-------- AppDelegate

    public void launch(Map<String, Object> options) {

        //
        // launch server
        //

        NetworkDatabase database = NetworkDatabase.getInstance();
        List<ProviderTable.ProviderInfo> providers = database.allProviders();
        if (providers != null && providers.size() > 0) {
            // choose the default sp
            ProviderTable.ProviderInfo sp = providers.get(0);
            List<ProviderTable.StationInfo> stations = database.allStations(sp.identifier);
            if (stations != null && stations.size() > 0) {
                // choose the default station
                ProviderTable.StationInfo srv = stations.get(0);
                startServer(srv);
            }
        }

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

    private boolean isServerPaused = false;

    public void enterBackground() {
        Server server = getCurrentServer();
        if (server != null) {
            User user = getCurrentUser();
            if (user != null) {
                // report client state
                Command cmd = new Command("broadcast");
                cmd.put("title", "report");
                cmd.put("state", "background");
                messenger.sendCommand(cmd);
            }

            // pause the server
            if (!isServerPaused) {
                server.pause();
                isServerPaused = true;
            }
        }
    }

    public void enterForeground() {
        Server server = getCurrentServer();
        if (server != null) {
            // resume the server
            if (isServerPaused) {
                server.resume();
                isServerPaused = false;
            }

            // clear icon badge

            User user = getCurrentUser();
            if (user != null) {
                // report client state
                Command cmd = new Command("broadcast");
                cmd.put("title", "report");
                cmd.put("state", "foreground");
                messenger.sendCommand(cmd);
            }
        }
    }

    //---- StationDelegate

    @Override
    public void onReceivePackage(byte[] data, Station server) {
        byte[] response;
        try {
            response = messenger.processPackage(data);
        } catch (NullPointerException e) {
            e.printStackTrace();
            response = null;
        }
        if (response != null && response.length > 0) {
            currentServer.sendPackage(response, null);
        }
    }

    @Override
    public void didSendPackage(byte[] data, Station server) {
        // TODO: mark it sent
    }

    @Override
    public void didFailToSendPackage(Error error, byte[] data, Station server) {
        // TODO: resend it
    }

    @Override
    public void onHandshakeAccepted(String session, Station server) {
        User user = getCurrentUser();
        assert user != null : "current user not found";

        // post current profile to station
        Profile profile = user.getProfile();
        if (profile != null) {
            messenger.postProfile(profile);
        }

        // broadcast login command
        LoginCommand login = new LoginCommand(user.identifier);
        login.setAgent(getUserAgent());
        login.setStation(server);
        // TODO: set provider
        messenger.broadcastContent(login);
    }
}
