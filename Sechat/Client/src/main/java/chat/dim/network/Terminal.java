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
import java.util.Locale;
import java.util.Map;

import chat.dim.User;
import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.database.ProviderTable;
import chat.dim.model.NetworkDatabase;
import chat.dim.protocol.ID;

public abstract class Terminal {

    private Server currentServer = null;

    public Terminal() {
        super();
        Messenger messenger = Messenger.getInstance();
        messenger.setTerminal(this);
    }

    public String getLanguage() {
        return Locale.getDefault().getLanguage();
    }

    // "DIM"
    public abstract String getDisplayName();

    // "1.0.1"
    public abstract String getVersionName();

    // "4.0"
    public abstract String getSystemVersion();

    // "HMS"
    public abstract String getSystemModel();

    // "hammerhead"
    public abstract String getSystemDevice();

    // "HUAWEI"
    public abstract String getDeviceBrand();

    // "hammerhead"
    public abstract String getDeviceBoard();

    // "HUAWEI"
    public abstract String getDeviceManufacturer();

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

    public Server getCurrentServer() {
        return currentServer;
    }
    private void setCurrentServer(Server server) {
        if (currentServer != server) {
            if (currentServer != null) {
                currentServer.end();
            }
            currentServer = server;
        }
    }
    private boolean isNewServer(String host, int port) {
        if (currentServer == null) {
            return true;
        }
        if (currentServer.getPort() != port) {
            return true;
        }
        String ip = currentServer.getHost();
        return ip == null || !ip.equals(host);
    }

    public User getCurrentUser() {
        if (currentServer == null) {
            return null;
        }
        return currentServer.getCurrentUser();
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

        Messenger messenger = Messenger.getInstance();
        Facebook facebook = messenger.getFacebook();

        // connect server
        if (isNewServer(host, port)) {
            // disconnect old server
            setCurrentServer(null);
            // connect new server
            Server server = new Server(identifier, host, port, name);
            server.setDataSource(facebook);
            server.setDelegate(messenger);
            server.start(options);
            setCurrentServer(server);
        }

        // get user from database and login
        User user = facebook.getCurrentUser();
        if (user != null) {
            currentServer.setCurrentUser(user);
            currentServer.handshake(null);
        }
    }

    public void startServer() {
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

        //
        // launch server
        //
        startServer();

        // TODO: notice("DocumentUpdated")

        // APNs?
        // Icon badge?
    }

    public void terminate() {
        setCurrentServer(null);
    }

    public void enterBackground() {
        if (currentServer != null) {
            // report client state
            Messenger messenger = Messenger.getInstance();
            messenger.reportOffline();

            // pause the server
            currentServer.pause();
        }
    }

    public void enterForeground() {
        if (currentServer != null) {
            // resume the server
            currentServer.resume();

            // report client state
            Messenger messenger = Messenger.getInstance();
            messenger.reportOnline();

            // TODO: clear icon badge
        }
    }
}
