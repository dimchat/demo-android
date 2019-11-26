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
package chat.dim.model;

import java.util.ArrayList;
import java.util.List;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.ReliableMessage;
import chat.dim.User;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.cpu.BlockCommandProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.MuteCommandProcessor;
import chat.dim.cpu.SearchCommandProcessor;
import chat.dim.cpu.StorageCommandProcessor;
import chat.dim.network.Server;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ProfileCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;

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
        Content content = msg.content;
        // TODO: check message type
        //       only save normal message and group commands
        //       ignore 'Handshake', ...
        //       return true to allow responding

        if (content instanceof HandshakeCommand) {
            // handshake command will be processed by CPUs
            // no need to save handshake command here
            return true;
        }
        if (content instanceof MetaCommand) {
            // meta & profile command will be checked and saved by CPUs
            // no need to save meta & profile command here
            return true;
        }
        if (content instanceof MuteCommand || content instanceof BlockCommand) {
            // TODO: create CPUs for mute & block command
            // no need to save mute & block command here
            return true;
        }
        if (content instanceof SearchCommand) {
            // search result will be parsed by CPUs
            // no need to save search command here
            return true;
        }

        Amanuensis clerk = Amanuensis.getInstance();

        if (content instanceof ReceiptCommand) {
            return clerk.saveReceipt(msg);
        } else {
            return clerk.saveMessage(msg);
        }
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
        CommandProcessor.register(Command.HANDSHAKE, HandshakeCommandProcessor.class);

        CommandProcessor.register(MuteCommand.MUTE, MuteCommandProcessor.class);
        CommandProcessor.register(BlockCommand.BLOCK, BlockCommandProcessor.class);

        // storage (contacts, private_key)
        CommandProcessor.register(StorageCommand.STORAGE, StorageCommandProcessor.class);
        CommandProcessor.register(StorageCommand.CONTACTS, StorageCommandProcessor.class);
        CommandProcessor.register(StorageCommand.PRIVATE_KEY, StorageCommandProcessor.class);

        CommandProcessor.register(SearchCommand.SEARCH, SearchCommandProcessor.class);
        CommandProcessor.register(SearchCommand.ONLINE_USERS, SearchCommandProcessor.class);

        // default content processor
        ContentProcessor.register(ContentType.UNKNOWN, AnyContentProcessor.class);
    }
}
