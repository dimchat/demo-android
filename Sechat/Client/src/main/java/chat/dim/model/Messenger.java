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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.ReliableMessage;
import chat.dim.User;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.cpu.SearchCommandProcessor;
import chat.dim.cpu.StorageCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.network.Server;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ProfileCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;

public class Messenger extends chat.dim.common.Messenger {
    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();
        setEntityDelegate(Facebook.getInstance());
    }

    public Server server = null;

    @Override
    public boolean saveMessage(InstantMessage<ID, SymmetricKey> iMsg) {
        chat.dim.Content content = iMsg.getContent();
        // TODO: check message type
        //       only save normal message and group commands
        //       ignore 'Handshake', ...
        //       return true to allow responding

        if (content instanceof HandshakeCommand) {
            // handshake command will be processed by CPUs
            // no need to save handshake command here
            return true;
        }
        if (content instanceof LoginCommand) {
            // login command will be processed by CPUs
            // no need to save login command here
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
        if (content instanceof ForwardContent) {
            // forward content will be parsed, if secret message decrypted, save it
            // no need to save forward content itself
            return true;
        }

        if (content instanceof InviteCommand) {
            // send keys again
            ID me = iMsg.envelope.getReceiver();
            ID group = (ID) content.getGroup();
            SymmetricKey key = getCipherKeyDelegate().getCipherKey(me, group);
            if (key != null) {
                //key.put("reused", null);
                key.remove("reused");
            }
        }
        if (content instanceof QueryCommand) {
            // FIXME: same query command sent to different members?
            return true;
        }

        Amanuensis clerk = Amanuensis.getInstance();

        if (content instanceof ReceiptCommand) {
            return clerk.saveReceipt(iMsg);
        } else {
            return clerk.saveMessage(iMsg);
        }
    }

    @Override
    public void suspendMessage(ReliableMessage msg) {
        // TODO: save this message in a queue waiting sender's meta response
    }

    @Override
    public void suspendMessage(InstantMessage msg) {
        // TODO: save this message in a queue waiting receiver's meta response
    }

    @Override
    protected Content process(Content content, ID sender, ReliableMessage<ID, SymmetricKey> rMsg) {
        Content res = super.process(content, sender, rMsg);
        if (res == null) {
            // respond nothing
            return null;
        }
        if (res instanceof HandshakeCommand) {
            // urgent command
            return res;
        }
        /*
        if (res instanceof ReceiptCommand) {
            ID receiver = getFacebook().getID(rMsg.envelope.receiver);
            if (NetworkType.Station.equals(receiver.getType())) {
                // no need to respond receipt to station
                return null;
            }
        }
         */

        // check receiver
        ID receiver = rMsg.envelope.getReceiver();
        User user = select(receiver);
        assert user != null : "receiver error: " + receiver;
        // pack message
        InstantMessage<ID, SymmetricKey> iMsg = new InstantMessage<>(res, user.identifier, sender);
        // normal response
        sendMessage(iMsg, null);
        // DON'T respond to station directly
        return null;
    }

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return true on success
     */
    public boolean sendCommand(Command cmd) {
        if (server == null) {
            return false;
        }
        return sendContent(cmd, server.identifier, null);
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     * @return true on success
     */
    public boolean broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        return sendContent(content, ID.EVERYONE, null);
    }

    public void broadcastProfile(Profile profile) {
        User user = server.getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        Facebook facebook = (Facebook) getFacebook();
        ID identifier = facebook.getID(profile.getIdentifier());
        assert identifier.equals(user.identifier) : "profile error: " + profile;
        // check profile
        if (facebook.isSigned(profile)) {
            profile.remove(chat.dim.common.Facebook.EXPIRES_KEY);
        } else {
            profile = null;
        }
        // pack and send profile to every contact
        Command cmd = new ProfileCommand(identifier, profile);
        List<ID> contacts = user.getContacts();
        if (contacts != null) {
            for (ID contact : contacts) {
                sendContent(cmd, contact, null);
            }
        }
    }

    public boolean postProfile(Profile profile) {
        return postProfile(profile, null);
    }

    public boolean postProfile(Profile profile, Meta meta) {
        ID identifier = ID.getInstance(profile.getIdentifier());
        // check profile
        Facebook facebook = (Facebook) getFacebook();
        if (facebook.isSigned(profile)) {
            profile.remove(chat.dim.common.Facebook.EXPIRES_KEY);
        } else {
            profile = null;
        }
        Command cmd = new ProfileCommand(identifier, meta, profile);
        return sendCommand(cmd);
    }

    public boolean postContacts(List<ID> contacts) {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        // 1. generate password
        SymmetricKey password;
        try {
            password = SymmetricKey.generate(SymmetricKey.AES);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        // 2. encrypt contacts list
        byte[] data = JSON.encode(contacts);
        data = password.encrypt(data);
        // 3. encrypt key
        byte[] key = JSON.encode(password);
        key = user.encrypt(key);
        // 4. pack 'storage' command
        StorageCommand cmd = new StorageCommand(StorageCommand.CONTACTS);
        cmd.setIdentifier(user.identifier);
        cmd.setData(data);
        cmd.setKey(key);
        return sendCommand(cmd);
    }

    public boolean queryContacts() {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        StorageCommand cmd = new StorageCommand(StorageCommand.CONTACTS);
        cmd.setIdentifier(user.identifier);
        return sendCommand(cmd);
    }

    private final Map<ID, Date> metaQueryTime = new HashMap<>();
    private final Map<ID, Date> profileQueryTime = new HashMap<>();
    private final Map<ID, Date> groupQueryTime = new HashMap<>();

    private static final int EXPIRES = 30 * 1000;  // 30 seconds

    public boolean queryMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            // broadcast ID has not meta
            return false;
        }

        // check for duplicated querying
        Date now = new Date();
        Date lastTime = metaQueryTime.get(identifier);
        if (lastTime != null && (now.getTime() - lastTime.getTime()) < EXPIRES) {
            return false;
        }
        metaQueryTime.put(identifier, now);

        // query from DIM network
        Command cmd = new MetaCommand(identifier);
        return sendCommand(cmd);
    }

    public boolean queryProfile(ID identifier) {
        // check for duplicated querying
        Date now = new Date();
        Date lastTime = profileQueryTime.get(identifier);
        if (lastTime != null && (now.getTime() - lastTime.getTime()) < EXPIRES) {
            return false;
        }
        profileQueryTime.put(identifier, now);

        // query from DIM network
        Command cmd = new ProfileCommand(identifier);
        return sendCommand(cmd);
    }

    public boolean queryGroupInfo(ID group, List<ID> members) {
        // check for duplicated querying
        Date now = new Date();
        Date lastTime = groupQueryTime.get(group);
        if (lastTime != null && (now.getTime() - lastTime.getTime()) < EXPIRES) {
            return false;
        }
        groupQueryTime.put(group, now);

        // query from members
        Command cmd = new QueryCommand(group);
        boolean checking = false;
        for (ID user : members) {
            if (sendContent(cmd, user, null)) {
                checking = true;
            }
        }
        return checking;
    }

    public boolean queryOnlineUsers() {
        Command cmd = new SearchCommand(SearchCommand.ONLINE_USERS);
        return sendCommand(cmd);
    }

    public boolean searchUsers(String keywords) {
        Command cmd = new SearchCommand(keywords);
        return sendCommand(cmd);
    }

    public boolean login(User user) {
        assert server != null : "server not connect yet";
        if (user == null) {
            user = getFacebook().getCurrentUser();
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

        CommandProcessor.register(Command.LOGIN, LoginCommandProcessor.class);

        // storage (contacts, private_key)
        CommandProcessor.register(StorageCommand.STORAGE, StorageCommandProcessor.class);
        CommandProcessor.register(StorageCommand.CONTACTS, StorageCommandProcessor.class);
        CommandProcessor.register(StorageCommand.PRIVATE_KEY, StorageCommandProcessor.class);

        CommandProcessor.register(SearchCommand.SEARCH, SearchCommandProcessor.class);
        CommandProcessor.register(SearchCommand.ONLINE_USERS, SearchCommandProcessor.class);
    }
}
