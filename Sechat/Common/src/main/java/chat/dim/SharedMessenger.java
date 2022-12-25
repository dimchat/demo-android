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
package chat.dim;

import com.alibaba.fastjson.JSONException;

import java.util.List;

import chat.dim.core.Session;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dbi.MessageDBI;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.Station;
import chat.dim.mkm.User;
import chat.dim.network.ClientSession;
import chat.dim.port.Departure;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.SecureMessage;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.Visa;
import chat.dim.type.Pair;
import chat.dim.utils.Log;

public class SharedMessenger extends ClientMessenger {

    public SharedMessenger(Session session, CommonFacebook facebook, MessageDBI database) {
        super(session, facebook, database);
    }

    public User getCurrentUser() {
        return getFacebook().getCurrentUser();
    }

    public Station getCurrentStation() {
        return getSession().getStation();
    }

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return true on success
     */
    public boolean sendCommand(Command cmd, int priority) {
        ID sid = getCurrentStation().getIdentifier();
        return sendContent(sid, cmd, priority);
    }

    private boolean sendContent(ID receiver, Content content, int priority) {
        ClientSession session = getSession();
        if (!session.isActive()) {
            return false;
        }
        Pair<InstantMessage, ReliableMessage> result;
        result = sendContent(null, receiver, content, priority);
        return result.second != null;
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     */
    public void broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        sendContent(ID.EVERYONE, content, 1);
    }

    public void broadcastVisa(Visa visa) {
        User user = getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new NullPointerException("login first");
        }
        ID identifier = visa.getIdentifier();
        if (!user.getIdentifier().equals(identifier)) {
            throw new IllegalArgumentException("visa document error: " + visa);
        }
        // pack and send user document to every contact
        List<ID> contacts = user.getContacts();
        if (contacts != null && contacts.size() > 0) {
            Command cmd = DocumentCommand.response(identifier, visa);
            for (ID contact : contacts) {
                sendContent(contact, cmd, 1);
            }
        }
    }

    public boolean postDocument(Document doc, Meta meta) {
        Command cmd = DocumentCommand.response(doc.getIdentifier(), meta, doc);
        return sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    public void postContacts(List<ID> contacts) {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        // 1. generate password
        SymmetricKey password = SymmetricKey.generate(SymmetricKey.AES);
        // 2. encrypt contacts list
        byte[] data = UTF8.encode(JSON.encode(contacts));
        data = password.encrypt(data);
        // 3. encrypt key
        byte[] key = UTF8.encode(JSON.encode(password));
        key = user.encrypt(key);
        // 4. pack 'storage' command
        StorageCommand cmd = new StorageCommand(StorageCommand.CONTACTS);
        cmd.setIdentifier(user.getIdentifier());
        cmd.setData(data);
        cmd.setKey(key);
        sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    public void queryContacts() {
        User user = getFacebook().getCurrentUser();
        assert user != null : "current user empty";
        StorageCommand cmd = new StorageCommand(StorageCommand.CONTACTS);
        cmd.setIdentifier(user.getIdentifier());
        sendCommand(cmd, Departure.Priority.SLOWER.value);
    }

    @Override
    public boolean queryDocument(ID identifier) {
        boolean ok = super.queryDocument(identifier);
        if (ok) {
            Log.info("querying document: " + identifier);
        } else {
            Log.info("document query not expired: " + identifier);
        }
        return ok;
    }

    public boolean queryGroupInfo(ID group, List<ID> members) {
        // TODO:
        return false;
    }

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        if (content instanceof Command) {
            content = Compatible.fixCommand((Command) content);
        }
        return super.serializeContent(content, password, iMsg);
    }

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        Content content;
        try {
            content = super.deserializeContent(data, password, sMsg);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        if (content == null) {
            throw new NullPointerException("failed to deserialize message content: " + sMsg);
        }
        if (content instanceof Command) {
            content = Compatible.fixCommand((Command) content);
        }
        return content;
    }

    static {

        //
        //  Register command parsers
        //

        // Mute
        Command.setFactory(MuteCommand.MUTE, MuteCommand::new);
        // Block
        Command.setFactory(BlockCommand.BLOCK, BlockCommand::new);

        // Report (online, offline)
        Command.setFactory("broadcast", ReportCommand::new);
        Command.setFactory(ReportCommand.ONLINE, ReportCommand::new);
        Command.setFactory(ReportCommand.OFFLINE, ReportCommand::new);

        // Storage (contacts, private_key)
        Command.setFactory(StorageCommand.STORAGE, StorageCommand::new);
        Command.setFactory(StorageCommand.CONTACTS, StorageCommand::new);
        Command.setFactory(StorageCommand.PRIVATE_KEY, StorageCommand::new);

        // Search (users)
        Command.setFactory(SearchCommand.SEARCH, SearchCommand::new);
        Command.setFactory(SearchCommand.ONLINE_USERS, SearchCommand::new);
    }
}