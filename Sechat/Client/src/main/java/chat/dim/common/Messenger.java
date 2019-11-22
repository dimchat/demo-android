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
package chat.dim.common;

import java.util.ArrayList;
import java.util.List;

import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.TextContentProcessor;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.mkm.ID;
import chat.dim.mkm.User;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.network.Server;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ProfileCommand;

public class Messenger extends chat.dim.Messenger {
    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();

        setSocialNetworkDataSource(Facebook.getInstance());
        setCipherKeyDataSource(KeyStore.getInstance());
    }

    public Server server = null;

    @Override
    public List<User> getLocalUsers() {
        List<User> users = super.getLocalUsers();
        if (users == null) {
            Facebook facebook = (Facebook) getFacebook();
            List<ID> allUsers = facebook.allUsers();
            users = new ArrayList<>();
            User user;
            for (ID item : allUsers) {
                user = facebook.getUser(item);
                if (user == null) {
                    throw new NullPointerException("failed to create user: " + item);
                }
                users.add(user);
            }
            setLocalUsers(users);
        }
        return users;
    }

    @Override
    public boolean saveMessage(InstantMessage msg) {
        Amanuensis clerk = Amanuensis.getInstance();
        return clerk.saveMessage(msg);
    }

    @Override
    public Content broadcastMessage(ReliableMessage msg) {
        return null;
    }

    @Override
    public Content deliverMessage(ReliableMessage msg) {
        return null;
    }

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return InstantMessage been sent
     */
    public boolean sendCommand(Command cmd) {
        assert server != null;
        return sendContent(cmd, server.identifier);
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     * @return InstantMessage been sent
     */
    public boolean broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        return sendContent(content, ID.ANYONE);
    }

    public void broadcastProfile(Profile profile) {
        User user = server.getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        ID identifier = ID.getInstance(profile.getIdentifier());
        assert identifier.equals(user.identifier);
        // pack and send profile to every contact
        Command cmd = new ProfileCommand(identifier, profile);
        List<ID> contacts = user.getContacts();
        for (ID contact : contacts) {
            sendContent(cmd, contact);
        }
    }

    public boolean postProfile(Profile profile) {
        return postProfile(profile, null);
    }

    public boolean postProfile(Profile profile, Meta meta) {
        ID identifier = ID.getInstance(profile.getIdentifier());
        Command cmd = new ProfileCommand(identifier, meta, profile);
        return sendCommand(cmd);
    }

    void postContacts(List<ID> contacts) {
        // TODO: encrypt contacts and send to station
    }

    @Override
    public boolean queryMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            return false;
        }
        Command cmd = new MetaCommand(identifier);
        return sendCommand(cmd);
    }

    public boolean queryProfile(ID identifier) {
        if (identifier.isBroadcast()) {
            return false;
        }
        Command cmd = new ProfileCommand(identifier);
        return sendCommand(cmd);
    }

    public boolean queryOnlineUsers() {
        Command cmd = new Command("users");
        return sendCommand(cmd);
    }

    public boolean searchUsers(String keywords) {
        Command cmd = new Command("search");
        cmd.put("keywords", keywords);
        return sendCommand(cmd);
    }

    public boolean login(User user) {
        assert server != null;
        if (user == null) {
            user = getCurrentUser();
            if (user == null) {
                // user not found
                return false;
            }
        }
        if (user.equals(server.getCurrentUser())) {
            // user not change
            return true;
        }
        // clear session
        server.session = null;

        server.setCurrentUser(user);

        server.handshake(null);
        return true;
    }

    //-------- Send


    static {
        // register CPUs
        ContentProcessor.register(ContentType.TEXT, TextContentProcessor.class);
        CommandProcessor.register(Command.HANDSHAKE, HandshakeCommandProcessor.class);
    }
}
