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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.Content;
import chat.dim.Envelope;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.ReliableMessage;
import chat.dim.SecureMessage;
import chat.dim.User;
import chat.dim.core.CipherKeyDelegate;
import chat.dim.core.EntityDelegate;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.cpu.BlockCommandProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.MuteCommandProcessor;
import chat.dim.cpu.ReceiptCommandProcessor;
import chat.dim.cpu.SearchCommandProcessor;
import chat.dim.cpu.StorageCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.format.JSON;
import chat.dim.network.Server;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ProfileCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.ResetCommand;

public class Messenger extends chat.dim.Messenger {
    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();

        setEntityDelegate(Facebook.getInstance());
        setCipherKeyDelegate(KeyStore.getInstance());
    }

    public Server server = null;

    // check whether group info empty
    private boolean isEmpty(ID group) {
        chat.dim.Facebook facebook = getFacebook();
        List members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            return true;
        }
        ID owner = facebook.getOwner(group);
        return owner == null;
    }

    // check whether need to update group
    private boolean checkGroup(Content content, ID sender) {
        // Check if it is a group message, and whether the group members info needs update
        chat.dim.Facebook facebook = getFacebook();
        ID group = facebook.getID(content.getGroup());
        if (group == null || group.isBroadcast()) {
            // 1. personal message
            // 2. broadcast message
            return false;
        }
        // check meta for new group ID
        Meta meta = facebook.getMeta(group);
        if (meta == null) {
            // NOTICE: if meta for group not found,
            //         facebook should query it from DIM network automatically
            // TODO: insert the message to a temporary queue to wait meta
            //throw new NullPointerException("group meta not found: " + group);
            return true;
        }
        // query group info
        if (isEmpty(group)) {
            // NOTICE: if the group info not found, and this is not an 'invite' command
            //         query group info from the sender
            if (content instanceof InviteCommand || content instanceof ResetCommand) {
                // FIXME: can we trust this stranger?
                //        may be we should keep this members list temporary,
                //        and send 'query' to the owner immediately.
                // TODO: check whether the members list is a full list,
                //       it should contain the group owner(owner)
                return false;
            } else {
                return queryGroupInfo(group, sender);
            }
        } else if (facebook.existsMember(sender, group)
                || facebook.existsAssistant(sender, group)
                || facebook.isOwner(sender, group)) {
            // normal membership
            return false;
        } else {

            // if assistants exists, query them
            List<ID> assistants = facebook.getAssistants(group);
            List<ID> admins = new ArrayList<>(assistants);
            // if owner found, query it too
            ID owner = facebook.getOwner(group);
            if (owner != null && !admins.contains(owner)) {
                admins.add(owner);
            }
            return queryGroupInfo(group, admins);
        }
    }

    @Override
    public SecureMessage encryptMessage(InstantMessage iMsg) {
        SecureMessage sMsg = super.encryptMessage(iMsg);

        EntityDelegate facebook = getEntityDelegate();
        Envelope env = iMsg.envelope;
        ID receiver = facebook.getID(env.receiver);
        if (receiver.isGroup()) {
            CipherKeyDelegate keyCache = getCipherKeyDelegate();
            // reuse group message keys
            ID sender = facebook.getID(env.sender);
            SymmetricKey key = keyCache.getCipherKey(sender, receiver);
            key.put("reused", true);
        }
        // TODO: reuse personal message key?

        return sMsg;
    }

    @Override
    public byte[] serializeKey(Map<String, Object> password, InstantMessage iMsg) {
        if (password.get("reused") != null) {
            // no need to encrypt reused key again
            return null;
        }
        return super.serializeKey(password, iMsg);
    }

    @Override
    public boolean saveMessage(InstantMessage iMsg) {
        Content content = iMsg.content;
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
        if (content instanceof ForwardContent) {
            // forward content will be parsed, if secret message decrypted, save it
            // no need to save forward content itself
            return true;
        }

        if (content instanceof InviteCommand) {
            // send keys again
            ID me = getFacebook().getID(iMsg.envelope.receiver);
            ID group = getFacebook().getID(content.getGroup());
            SymmetricKey key = getCipherKeyDelegate().getCipherKey(me, group);
            key.put("reused", null);
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
    public InstantMessage process(InstantMessage iMsg) {
        Content content = iMsg.content;
        ID sender = getFacebook().getID(iMsg.envelope.sender);

        if (checkGroup(content, sender)) {
            // save this message in a queue to wait group meta response
            suspendMessage(iMsg);
            return null;
        }

        iMsg = super.process(iMsg);
        if (iMsg == null) {
            // respond nothing
            return null;
        }
        if (iMsg.content instanceof HandshakeCommand) {
            // urgent command
            return iMsg;
        }
        /*
        if (iMsg.content instanceof ReceiptCommand) {
            ID receiver = getFacebook().getID(iMsg.envelope.receiver);
            if (NetworkType.Station.equals(receiver.getType())) {
                // no need to respond receipt to station
                return null;
            }
        }
         */
        // normal response
        sendMessage(iMsg, null, false);
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
        assert server != null;
        return sendContent(cmd, server.identifier, null, false);
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     * @return true on success
     */
    public boolean broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        return sendContent(content, ID.ANYONE, null, false);
    }

    public void broadcastProfile(Profile profile) {
        User user = server.getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        ID identifier = getFacebook().getID(profile.getIdentifier());
        assert identifier.equals(user.identifier);
        // pack and send profile to every contact
        Command cmd = new ProfileCommand(identifier, profile);
        List<ID> contacts = user.getContacts();
        for (ID contact : contacts) {
            sendContent(cmd, contact, null, false);
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

    private byte[] jsonEncode(Object container) {
        String json = JSON.encode(container);
        return json.getBytes(Charset.forName("UTF-8"));
    }

    public boolean postContacts(List<ID> contacts) {
        User user = getFacebook().getCurrentUser();
        assert user != null;
        // 1. generate password
        SymmetricKey password;
        try {
            password = SymmetricKey.generate(SymmetricKey.AES);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
        // 2. encrypt contacts list
        byte[] data = jsonEncode(contacts);
        data = password.encrypt(data);
        // 3. encrypt key
        byte[] key = jsonEncode(password);
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
        assert user != null;
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
            if (sendContent(cmd, user, null, false)) {
                checking = true;
            }
        }
        return checking;
    }

    public boolean queryGroupInfo(ID group, ID member) {
        List<ID> array = new ArrayList<>();
        array.add(member);
        return queryGroupInfo(group, array);
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
        assert server != null;
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
        CommandProcessor.register(Command.RECEIPT, ReceiptCommandProcessor.class);

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
