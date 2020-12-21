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
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.stargate.StarShip;
import chat.dim.utils.Log;

public final class Messenger extends chat.dim.common.Messenger {

    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();

        // set Facebook as Entity Delegate
        setEntityDelegate(Facebook.getInstance());

        // set Data Source
        setDataSource(MessageDataSource.getInstance());
    }

    @Override
    public Facebook getFacebook() {
        return (Facebook) super.getFacebook();
    }

    @Override
    protected MessageProcessor getMessageProcessor() {
        return (MessageProcessor) super.getMessageProcessor();
    }
    @Override
    protected MessageProcessor newMessageProcessor() {
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

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return true on success
     */
    @Override
    public boolean sendCommand(Command cmd, int priority) {
        Server server = getCurrentServer();
        if (server == null) {
            return false;
        }
        return sendContent(null, server.identifier, cmd, null, priority);
    }

    @Override
    public boolean sendContent(ID sender, ID receiver, Content content, Messenger.Callback callback, int priority) {
        if (sender == null) {
            User user = getCurrentUser();
            if (user == null) {
                // FIXME: suspend message for waiting user login
                return false;
            }
            sender = user.identifier;
        }
        return super.sendContent(sender, receiver, content, callback, priority);
    }

    private boolean sendContent(ID receiver, Content content) {
        return sendContent(null, receiver, content, null, StarShip.SLOWER);
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

    public void broadcastProfile(Document profile) {
        // check profile
        Facebook facebook = getFacebook();
        if (facebook.isSigned(profile)) {
            profile.remove(chat.dim.common.Facebook.EXPIRES_KEY);
        } else {
            return;
        }
        User user = getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new NullPointerException("login first");
        }
        ID identifier = profile.getIdentifier();
        if (!user.identifier.equals(identifier)) {
            throw new IllegalArgumentException("profile error: " + profile);
        }
        // pack and send profile to every contact
        Command cmd = new DocumentCommand(identifier, profile);
        List<ID> contacts = user.getContacts();
        if (contacts != null) {
            for (ID contact : contacts) {
                sendContent(contact, cmd);
            }
        }
    }

    public boolean postProfile(Document profile, Meta meta) {
        ID identifier = profile.getIdentifier();
        // check profile
        Facebook facebook = getFacebook();
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

    private static final int QUERY_INTERVAL = 120 * 1000;  // query interval (2 minutes)

    @Override
    public boolean queryMeta(ID identifier) {
        if (ID.isBroadcast(identifier)) {
            // broadcast ID has no meta
            return false;
        }

        // check for duplicated querying
        long now = (new Date()).getTime();
        Number expires = metaQueryTime.get(identifier);
        if (expires != null && now < expires.longValue()) {
            return false;
        }
        metaQueryTime.put(identifier, now + QUERY_INTERVAL);
        Log.info("querying meta: " + identifier);

        // query from DIM network
        Command cmd = new MetaCommand(identifier);
        return sendCommand(cmd, StarShip.SLOWER);
    }

    @Override
    public boolean queryProfile(ID identifier) {
        if (ID.isBroadcast(identifier)) {
            // broadcast ID has no profile
            return false;
        }

        // check for duplicated querying
        long now = (new Date()).getTime();
        Number expires = profileQueryTime.get(identifier);
        if (expires != null && now < expires.longValue()) {
            return false;
        }
        profileQueryTime.put(identifier, now + QUERY_INTERVAL);
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
        groupQueryTime.put(group, now + QUERY_INTERVAL);

        // query from members
        Command cmd = new QueryCommand(group);
        boolean checking = false;
        for (ID user : members) {
            if (sendContent(user, cmd)) {
                checking = true;
            }
        }
        return checking;
    }
}
