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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.User;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.cpu.SearchCommandProcessor;
import chat.dim.cpu.StorageCommandProcessor;
import chat.dim.crypto.KeyFactory;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.mkm.BroadcastAddress;
import chat.dim.network.Server;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.stargate.StarShip;
import chat.dim.utils.Log;

public final class Messenger extends chat.dim.common.Messenger implements Observer {
    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();
        setEntityDelegate(Facebook.getInstance());
        setMessageProcessor(new MessageProcessor(this));

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MetaSaved);
    }

    @Override
    public void finalize() throws Throwable {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.MetaSaved);
        super.finalize();
    }

    public Server server = null;

    private final Map<ID, List<ReliableMessage>> incomingMessages = new HashMap<>();
    private final ReadWriteLock incomingMessageLock = new ReentrantReadWriteLock();

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MetaSaved)) {
            ID entity = (ID) info.get("ID");
            // purge incoming messages waiting for this ID's meta
            ReliableMessage rMsg;
            byte[] response;
            while ((rMsg = getIncomingMessage(entity)) != null) {
                rMsg = getMessageProcessor().process(rMsg);
                if (rMsg == null) {
                    continue;
                }
                response = serializeMessage(rMsg);
                if (response != null && response.length > 0) {
                    getDelegate().sendPackage(response, null, StarShip.SLOWER);
                }
            }
        }
    }

    private void addIncomingMessage(ReliableMessage rMsg, ID waiting) {
        Lock writeLock = incomingMessageLock.writeLock();
        writeLock.lock();
        try {
            List<ReliableMessage> messages = incomingMessages.get(waiting);
            if (messages == null) {
                messages = new ArrayList<>();
                incomingMessages.put(waiting, messages);
            }
            messages.add(rMsg);
        } finally {
            writeLock.unlock();
        }
    }
    private ReliableMessage getIncomingMessage(ID waiting) {
        ReliableMessage rMsg = null;
        Lock writeLock = incomingMessageLock.writeLock();
        writeLock.lock();
        try {
            List<ReliableMessage> messages = incomingMessages.get(waiting);
            if (messages != null && messages.size() > 0) {
                Log.info("==== processing incoming message(s): " + messages.size() + ", " + waiting);
                rMsg = messages.remove(0);
            }
        } finally {
            writeLock.unlock();
        }
        return rMsg;
    }

    @Override
    public void suspendMessage(ReliableMessage msg) {
        // save this message in a queue waiting sender's meta response
        ID waiting = (ID) msg.get("waiting");
        if (waiting == null) {
            waiting = msg.getSender();
        } else {
            msg.remove("waiting");
        }
        addIncomingMessage(msg, waiting);
    }

    @Override
    public void suspendMessage(InstantMessage msg) {
        // TODO: save this message in a queue waiting receiver's meta response
    }

    @Override
    public boolean saveMessage(InstantMessage iMsg) {
        Content content = iMsg.getContent();
        // TODO: check message type
        //       only save normal message and group commands
        //       ignore 'Handshake', ...
        //       return true to allow responding

        if (content instanceof HandshakeCommand) {
            // handshake command will be processed by CPUs
            // no need to save handshake command here
            return true;
        }
        if (content instanceof ReportCommand) {
            // report command is sent to station,
            // no need to save report command here
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
            ID me = iMsg.getReceiver();
            ID group = content.getGroup();
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

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return true on success
     */
    @Override
    public boolean sendCommand(Command cmd, int priority) {
        if (server == null) {
            return false;
        }
        return sendContent(cmd, server.identifier, null, priority);
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     */
    public void broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        sendContent(content, ID.EVERYONE, null, StarShip.SLOWER);
    }

    public void broadcastProfile(Document profile) {
        User user = server.getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        Facebook facebook = (Facebook) getFacebook();
        ID identifier = profile.getIdentifier();
        assert identifier.equals(user.identifier) : "profile error: " + profile;
        // check profile
        if (facebook.isSigned(profile)) {
            profile.remove(chat.dim.common.Facebook.EXPIRES_KEY);
        } else {
            profile = null;
        }
        // pack and send profile to every contact
        Command cmd = new DocumentCommand(identifier, profile);
        List<ID> contacts = user.getContacts();
        if (contacts != null) {
            for (ID contact : contacts) {
                sendContent(cmd, contact, null, StarShip.SLOWER);
            }
        }
    }

    public boolean postProfile(Document profile, Meta meta) {
        ID identifier = profile.getIdentifier();
        // check profile
        Facebook facebook = (Facebook) getFacebook();
        if (facebook.isSigned(profile)) {
            profile.remove(chat.dim.common.Facebook.EXPIRES_KEY);
        } else {
            profile = null;
        }
        Command cmd = new DocumentCommand(identifier, meta, profile);
        return sendCommand(cmd, StarShip.SLOWER);
    }

    public void postContacts(List<ID> contacts) {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        // 1. generate password
        SymmetricKey password = KeyFactory.getSymmetricKey(SymmetricKey.AES);
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
        sendCommand(cmd, StarShip.SLOWER);
    }

    public void queryContacts() {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        StorageCommand cmd = new StorageCommand(StorageCommand.CONTACTS);
        cmd.setIdentifier(user.identifier);
        sendCommand(cmd, StarShip.SLOWER);
    }

    private final Map<ID, Long> metaQueryTime = new HashMap<>();
    private final Map<ID, Long> profileQueryTime = new HashMap<>();
    private final Map<ID, Long> groupQueryTime = new HashMap<>();

    private static final int EXPIRES = 120 * 1000;  // query expires (2 minutes)

    @Override
    public boolean queryMeta(ID identifier) {
        if (identifier.getAddress() instanceof BroadcastAddress) {
            // broadcast ID has no meta
            return false;
        }

        // check for duplicated querying
        long now = (new Date()).getTime();
        Number expires = metaQueryTime.get(identifier);
        if (expires != null && now < expires.longValue()) {
            return false;
        }
        metaQueryTime.put(identifier, now + EXPIRES);
        Log.info("querying meta: " + identifier);

        // query from DIM network
        Command cmd = new MetaCommand(identifier);
        return sendCommand(cmd, StarShip.SLOWER);
    }

    @Override
    public boolean queryProfile(ID identifier) {
        if (identifier.getAddress() instanceof BroadcastAddress) {
            // broadcast ID has no profile
            return false;
        }

        // check for duplicated querying
        long now = (new Date()).getTime();
        Number expires = profileQueryTime.get(identifier);
        if (expires != null && now < expires.longValue()) {
            return false;
        }
        profileQueryTime.put(identifier, now + EXPIRES);
        Log.info("querying profile: " + identifier);

        // query from DIM network
        Command cmd = new DocumentCommand(identifier);
        return sendCommand(cmd, StarShip.SLOWER);
    }

    @Override
    public boolean queryGroupInfo(ID group, List<ID> members) {
        if (group.equals(ID.EVERYONE)) {
            // this group contains all users
            return false;
        }

        // check for duplicated querying
        long now = (new Date()).getTime();
        Number expires = groupQueryTime.get(group);
        if (expires != null && now < expires.longValue()) {
            return false;
        }
        groupQueryTime.put(group, now + EXPIRES);

        // query from members
        Command cmd = new QueryCommand(group);
        boolean checking = false;
        for (ID user : members) {
            if (sendContent(cmd, user, null, StarShip.SLOWER)) {
                checking = true;
            }
        }
        return checking;
    }

    public boolean queryOnlineUsers() {
        Command cmd = new SearchCommand(SearchCommand.ONLINE_USERS);
        return sendCommand(cmd, StarShip.NORMAL);
    }

    public boolean searchUsers(String keywords) {
        Command cmd = new SearchCommand(keywords);
        return sendCommand(cmd, StarShip.NORMAL);
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
