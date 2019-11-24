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
import java.util.List;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.protocol.Command;

public class Terminal implements StationDelegate {

    private Facebook facebook = Facebook.getInstance();
    private Messenger messenger = Messenger.getInstance();

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
        server.messenger = messenger;
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

    public List<User> allUsers() {
        if (users == null) {
            users = new ArrayList<>();
            List<ID> list = facebook.allUsers();
            User user;
            for (ID item : list) {
                user = facebook.getUser(item);
                if (user == null) {
                    throw new NullPointerException("failed to get local user: " + item);
                }
                users.add(user);
            }
        }
        return users;
    }

    //---- Content/processor and deliver

    private void sendContent(Content content, ID receiver) {
        messenger.sendContent(content, receiver);
    }

    protected void sendCommand(Command cmd) {
        messenger.sendCommand(cmd);
    }

    public void queryMeta(ID identifier) {
        messenger.queryMeta(identifier);
    }

    protected void login(User user) {
        messenger.login(user);
    }

    //---- StationDelegate

    @Override
    public void didSendPackage(byte[] data, Station server) {
        // TODO: mark it sent
    }

    @Override
    public void didFailToSendPackage(Error error, byte[] data, Station server) {
        // TODO: resend it
    }
}
