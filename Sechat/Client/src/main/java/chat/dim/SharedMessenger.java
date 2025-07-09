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

import java.util.ArrayList;
import java.util.List;

import chat.dim.compat.Compatible;
import chat.dim.crypto.SymmetricAlgorithms;
import chat.dim.crypto.SymmetricKey;
import chat.dim.dbi.MessageDBI;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.mkm.Station;
import chat.dim.mkm.User;
import chat.dim.mtp.MsgUtils;
import chat.dim.network.ClientSession;
import chat.dim.port.Departure;
import chat.dim.protocol.AnsCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Document;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.Visa;
import chat.dim.type.Pair;

public class SharedMessenger extends ClientMessenger {

    public static final int MTP_JSON = 0x01;
    public static final int MTP_DMTP = 0x02;

    // Message Transfer Protocol
    public int mtpFormat = MTP_JSON;

    public SharedMessenger(Session session, CommonFacebook facebook, MessageDBI database) {
        super(session, facebook, database);
    }

    public User getCurrentUser() {
        return getFacebook().getCurrentUser();
    }

    public Station getCurrentStation() {
        return getSession().getStation();
    }

    @Override
    public byte[] serializeMessage(ReliableMessage rMsg) {
        if (mtpFormat == MTP_JSON) {
            // JsON
            return super.serializeMessage(rMsg);
        } else {
            // D-MTP
            Compatible.fixMetaAttachment(rMsg);
            Compatible.fixVisaAttachment(rMsg);
            // TODO: attachKeyDigest(rMsg, getMessenger());
            return MsgUtils.serializeMessage(rMsg);
        }
    }

    @Override
    public ReliableMessage deserializeMessage(byte[] data) {
        if (data == null || data.length < 2) {
            return null;
        }
        ReliableMessage rMsg;
        if (data[0] == '{') {
            // JsON
            rMsg = super.deserializeMessage(data);
        } else {
            // D-MTP
            rMsg = MsgUtils.deserializeMessage(data);
            if (rMsg != null) {
                Compatible.fixMetaAttachment(rMsg);
                Compatible.fixVisaAttachment(rMsg);
                // FIXME: just change it when first package received
                mtpFormat = MTP_DMTP;
            }
        }
        return rMsg;
    }

    /**
     *  Pack and send command to station
     *
     * @param content  - command sending to the neighbor station
     * @param priority - task priority, smaller is faster
     * @return true on success
     */
    public boolean sendCommand(Command content, int priority) {
        ID sid = getCurrentStation().getIdentifier();
        return sendContent(sid, content, priority);
    }

    private boolean sendContent(ID receiver, Content content, int priority) {
        ClientSession session = getSession();
        if (!session.isActive()) {
            return false;
        }
        Pair<InstantMessage, ReliableMessage> result;
        result = sendContent(content, null, receiver, priority);
        return result.second != null;
    }

//    /**
//     *  Pack and broadcast content to everyone
//     *
//     * @param content - message content
//     */
//    public boolean broadcastContent(Content content) {
//        ID group = content.getGroup();
//        if (group == null || !group.isBroadcast()) {
//            group = ID.EVERYONE;
//            content.setGroup(group);
//        }
//        return sendContent(group, content, 1);
//    }

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
            Content content = DocumentCommand.response(identifier, visa);
            for (ID contact : contacts) {
                sendContent(contact, content, 1);
            }
        }
    }

    public boolean postDocument(Document doc, Meta meta) {
        Command content = DocumentCommand.response(doc.getIdentifier(), meta, doc);
        return sendCommand(content, Departure.Priority.SLOWER.value);
    }

    public void postContacts(List<ID> contacts) {
        User user = getCurrentUser();
        assert user != null : "current user empty";
        StorageCommand content = new StorageCommand(StorageCommand.CONTACTS);
        content.setIdentifier(user.getIdentifier());
        // 1. generate password
        SymmetricKey password = SymmetricKey.generate(SymmetricAlgorithms.AES);
        // 2. encrypt contacts list
        byte[] data = UTF8.encode(JSON.encode(contacts));
        data = password.encrypt(data, content.toMap());
        // 3. encrypt key
        byte[] key = UTF8.encode(JSON.encode(password));
        key = user.encrypt(key);
        // 4. pack 'storage' command
        content.setData(data);
        content.setKey(key);
        sendCommand(content, Departure.Priority.SLOWER.value);
    }

    public void queryContacts() {
        User user = getCurrentUser();
        assert user != null : "current user empty";
        StorageCommand content = new StorageCommand(StorageCommand.CONTACTS);
        content.setIdentifier(user.getIdentifier());
        sendCommand(content, Departure.Priority.SLOWER.value);
    }

    @Override
    public void handshakeSuccess() {
        super.handshakeSuccess();
        // query bot ID
        List<String> names = new ArrayList<>();
        names.add("archivist");
        names.add("assistant");
        AnsCommand command = AnsCommand.query(names);
        sendCommand(command, 1);
    }

    static {

        //
        //  Register command parsers
        //

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
