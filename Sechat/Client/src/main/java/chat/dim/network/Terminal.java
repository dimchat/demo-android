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
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.model.NetworkDatabase;
import chat.dim.protocol.Command;
import chat.dim.protocol.LoginCommand;

public class Terminal implements StationDelegate {

    protected Facebook facebook = Facebook.getInstance();
    protected Messenger messenger = Messenger.getInstance();

    private Server currentServer = null;

    private List<User> users = null;

    public Terminal() {
        super();
    }

    /**
     *  format: "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) DIMCoreKit/1.0 (Terminal, like WeChat) DIM-by-GSP/1.0.1"
     */
    public String getUserAgent() {
        return "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) " +
                "DIMCoreKit/1.0 (Terminal, like WeChat) " +
                "DIM-by-GSP/1.0.1";
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

    private void setCurrentUser(User user) {
        if (currentServer != null) {
            currentServer.setCurrentUser(user);
        }
        // TODO: update users list
    }

    //--------

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
        Server server = new Server(identifier, host, port);
        server.delegate = this;
        server.start(station);
        setCurrentServer(server);

        // get user from database and login
        messenger.login(null);
    }

    @SuppressWarnings("unchecked")
    private void launchServiceProvider(Map<String, Object> spConfig) {
        Facebook facebook = Facebook.getInstance();
        ID spID = facebook.getID(spConfig.get("ID"));
        ServiceProvider sp = new ServiceProvider(spID);

        List<Map<String, Object>> stations = (List) spConfig.get("stations");
        if (stations == null) {
            stations = NetworkDatabase.getInstance().allStations(spID);
        }
        if (stations == null || stations.size() == 0) {
            // TODO: waiting for permission.READ_EXTERNAL_STORAGE
            throw new NullPointerException("failed to get stations");
        } else {
            // choose the fast station
            Map<String, Object> neighbor = new HashMap<>(stations.get(0));
            startServer(neighbor, sp);
        }
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
            messenger.sendCommand(cmd);
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
            messenger.sendCommand(cmd);
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
            currentServer.star.send(response);
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
        // post contacts(encrypted) to station
        List<ID> contacts = user.getContacts();
        if (contacts != null && contacts.size() > 0) {
            messenger.postContacts(contacts);
        }
        // broadcast login command
        LoginCommand login = new LoginCommand(user.identifier);
        login.setAgent(getUserAgent());
        login.setStation(server);
        // TODO: set provider
        messenger.broadcastContent(login);
    }
}
