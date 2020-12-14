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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import chat.dim.User;
import chat.dim.database.ProviderTable;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.model.NetworkDatabase;
import chat.dim.protocol.Command;
import chat.dim.protocol.Document;
import chat.dim.protocol.ID;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.ReportCommand;
import chat.dim.stargate.StarShip;

public class Terminal implements Station.Delegate {

    protected Facebook facebook = Facebook.getInstance();
    protected Messenger messenger = Messenger.getInstance();

    private Server currentServer = null;

    public Terminal() {
        super();
    }

    public String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    public String getDisplayName() {
        // TODO: override me
        return "DIM";
    }

    public String getVersionName() {
        // TODO: override me
        return "1.0.1";
    }

    public String getSystemVersion() {
        // TODO: override me
        return "4.0";
    }

    public String getSystemModel() {
        // TODO: override me
        return "HMS";
    }

    public String getSystemDevice() {
        // TODO: override me
        return "hammerhead";
    }

    public String getDeviceBrand() {
        // TODO: override me
        return "HUAWEI";
    }

    public String getDeviceBoard() {
        // TODO: override me
        return "hammerhead";
    }

    public String getDeviceManufacturer() {
        // TODO: override me
        return "HUAWEI";
    }

    /**
     *  format: "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) DIMCoreKit/1.0 (Terminal, like WeChat) DIM-by-GSP/1.0.1"
     */
    public String getUserAgent() {
        String model = getSystemModel();
        String device = getSystemDevice();
        String sysVersion = getSystemVersion();
        String lang = getLanguage();

        String appName = getDisplayName();
        String appVersion = getVersionName();

        return String.format("DIMP/1.0 (%s; U; %s %s; %s)" +
                        " DIMCoreKit/1.0 (Terminal, like WeChat) %s-by-MOKY/%s",
                model, device, sysVersion, lang, appName, appVersion);
    }

    protected Server getCurrentServer() {
        return currentServer;
    }

    protected void setCurrentServer(Server server) {
        server.setDelegate(this);
        messenger.server = server;
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
            if (server != null) {
                server.end();
            }
            server = new Server(identifier, host, port, name);
            server.setDataSource(Facebook.getInstance());
            server.setDelegate(this);
            server.start(options);
            setCurrentServer(server);
        }

        // get user from database and login
        messenger.login(null);
    }

    public void startServer() {
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
    }

    //-------- AppDelegate

    public void launch(Map<String, Object> options) {

        //
        // launch server
        //
        startServer();

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

    private Date offlineTime = null;

    private void reportOnline() {
        Command cmd = new ReportCommand(ReportCommand.ONLINE);
        Date now = new Date();
        cmd.put("time", now.getTime() / 1000);
        if (offlineTime != null) {
            cmd.put("last_time", offlineTime.getTime() / 1000);
        }
        messenger.sendCommand(cmd, StarShip.NORMAL);
    }
    private void reportOffline() {
        User user = getCurrentUser();
        if (user == null) {
            return;
        }
        // report client state
        Command cmd = new ReportCommand(ReportCommand.OFFLINE);
        cmd.put("time", offlineTime.getTime() / 1000);
        messenger.sendCommand(cmd, StarShip.NORMAL);
    }

    public void enterBackground() {
        offlineTime = new Date();
        Server server = getCurrentServer();
        if (server != null) {
            // report client state
            reportOffline();

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

            // try to activate the connection
            if (server.getCurrentUser() != null) {
                server.handshake(null);
            }
        }
    }

    //---- Station Delegate

    @Override
    public void onReceivePackage(byte[] data, Station server) {
        byte[] response;
        try {
            response = messenger.process(data);
        } catch (NullPointerException e) {
            e.printStackTrace();
            response = null;
        }
        if (response != null && response.length > 0) {
            currentServer.sendPackage(response, null, StarShip.SLOWER);
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
    public void onHandshakeAccepted(Station server) {
        User user = getCurrentUser();
        assert user != null : "current user not found";

        // post current profile to station
        Document profile = user.getDocument(Document.PROFILE);
        if (!facebook.isEmpty(profile)) {
            messenger.postProfile(profile, null);
        }

        // report client state
        reportOnline();

        // broadcast login command
        LoginCommand login = new LoginCommand(user.identifier);
        login.setAgent(getUserAgent());
        login.setStation(server);
        // TODO: set provider
        messenger.broadcastContent(login);
    }
}
