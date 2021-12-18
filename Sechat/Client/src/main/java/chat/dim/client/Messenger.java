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
package chat.dim.client;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.User;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.model.MessageDataSource;
import chat.dim.network.Server;
import chat.dim.network.Terminal;
import chat.dim.port.Departure;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.Visa;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.utils.Log;

public final class Messenger extends chat.dim.common.Messenger {

    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();
    }

    @Override
    public Facebook getFacebook() {
        return (Facebook) super.getFacebook();
    }

    @Override
    protected MessageProcessor createMessageProcessor() {
        return new MessageProcessor(this);
    }

    private WeakReference<Terminal> terminalRef = null;

    public void setTerminal(Terminal client) {
        terminalRef = new WeakReference<>(client);
    }
    private Terminal getTerminal() {
        return terminalRef.get();
    }

    public Server getCurrentServer() {
        return getTerminal().getCurrentServer();
    }
    public User getCurrentUser() {
        return getTerminal().getCurrentUser();
    }

    @Override
    public void suspendMessage(ReliableMessage rMsg) {
        MessageDataSource ds = MessageDataSource.getInstance();
        ds.suspendMessage(rMsg);
    }

    @Override
    public void suspendMessage(InstantMessage iMsg) {
        MessageDataSource ds = MessageDataSource.getInstance();
        ds.suspendMessage(iMsg);
    }

    @Override
    public boolean saveMessage(InstantMessage iMsg) {
        MessageDataSource ds = MessageDataSource.getInstance();
        return ds.saveMessage(iMsg);
    }

    public boolean sendMessage(ReliableMessage rMsg, int priority) {
        Server server = getCurrentServer();
        return server.sendMessage(rMsg, priority);
    }

    public boolean sendMessage(InstantMessage iMsg, int priority) {
        Server server = getCurrentServer();
        return server.sendMessage(iMsg, priority);
    }

    @Override
    public boolean sendContent(ID sender, ID receiver, Content content, int priority) {
        // Application Layer should make sure user is already login before it send message to server.
        // Application layer should put message into queue so that it will send automatically after user login
        Server server = getCurrentServer();
        return server.sendContent(sender, receiver, content, priority);
    }

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return true on success
     */
    public boolean sendCommand(Command cmd, int priority) {
        Server server = getCurrentServer();
        if (server == null) {
            return false;
        }
        return sendContent(null, server.identifier, cmd, priority);
    }

    private boolean sendContent(ID receiver, Content content) {
        return sendContent(null, receiver, content, Departure.Priority.SLOWER.value);
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     */
    public void broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        sendContent(ID.EVERYONE, content);
    }

    public void broadcastVisa(Visa visa) {
        User user = getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new NullPointerException("login first");
        }
        ID identifier = visa.getIdentifier();
        if (!user.identifier.equals(identifier)) {
            throw new IllegalArgumentException("visa document error: " + visa);
        }
        visa.remove(chat.dim.common.Facebook.EXPIRES_KEY);
        // pack and send user document to every contact
        List<ID> contacts = user.getContacts();
        if (contacts != null && contacts.size() > 0) {
            Command cmd = new DocumentCommand(identifier, visa);
            for (ID contact : contacts) {
                sendContent(contact, cmd);
            }
        }
    }

    public boolean postDocument(Document doc, Meta meta) {
        doc.remove(chat.dim.common.Facebook.EXPIRES_KEY);
        Command cmd = new DocumentCommand(doc.getIdentifier(), meta, doc);
        return sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    public void postContacts(List<ID> contacts) {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        // 1. generate password
        SymmetricKey password = SymmetricKey.generate(SymmetricKey.AES);
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
        sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    public void queryContacts() {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        StorageCommand cmd = new StorageCommand(StorageCommand.CONTACTS);
        cmd.setIdentifier(user.identifier);
        sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    private final Map<ID, Long> metaQueryExpires = new HashMap<>();
    private final Map<ID, Long> documentQueryExpires = new HashMap<>();
    private final Map<ID, Map<ID, Long>> groupQueryExpires = new HashMap<>();

    private static final int QUERY_INTERVAL = 120 * 1000;  // query interval (2 minutes)

    @Override
    public boolean queryMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            // broadcast ID has no meta
            return false;
        }

        // check for duplicated querying
        long now = (new Date()).getTime();
        Number expires = metaQueryExpires.get(identifier);
        if (expires != null && now < expires.longValue()) {
            return false;
        }
        metaQueryExpires.put(identifier, now + QUERY_INTERVAL);
        Log.info("querying meta: " + identifier);

        // query from DIM network
        Command cmd = new MetaCommand(identifier);
        return sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    @Override
    public boolean queryDocument(ID identifier, String type) {
        if (identifier.isBroadcast()) {
            // broadcast ID has no document
            return false;
        }

        // check for duplicated querying
        long now = (new Date()).getTime();
        Number expires = documentQueryExpires.get(identifier);
        if (expires != null && now < expires.longValue()) {
            return false;
        }
        documentQueryExpires.put(identifier, now + QUERY_INTERVAL);
        Log.info("querying entity document: " + identifier);

        // query from DIM network
        Command cmd = new DocumentCommand(identifier);
        return sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    @Override
    public boolean queryGroupInfo(ID group, List<ID> members) {
        if (group.isBroadcast()) {
            // this group contains all users
            return false;
        }
        if (members.size() == 0) {
            return false;
        }

        Map<ID, Long> times = groupQueryExpires.get(group);
        if (times == null) {
            times = new HashMap<>();
            groupQueryExpires.put(group, times);
        }
        long now = (new Date()).getTime();

        // query from members
        Command cmd = new QueryCommand(group);
        boolean checking = false;
        Number expires;
        for (ID user : members) {
            // check for duplicated querying
            expires = times.get(user);
            if (expires != null && now < expires.longValue()) {
                continue;
            }
            times.put(user, now + QUERY_INTERVAL);
            Log.info("querying group: " + group + " from: " + user);

            if (sendContent(user, cmd)) {
                checking = true;
            }
        }
        return checking;
    }
}
