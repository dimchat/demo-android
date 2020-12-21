/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.common.KeyStore;
import chat.dim.crypto.SymmetricKey;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Content;
import chat.dim.protocol.ForwardContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.MetaCommand;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.stargate.StarShip;
import chat.dim.utils.Log;

public class MessageDataSource implements Messenger.DataSource, Observer {

    private static final MessageDataSource ourInstance = new MessageDataSource();
    public static MessageDataSource getInstance() { return ourInstance; }
    private MessageDataSource() {
        super();

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MetaSaved);
        nc.addObserver(this, NotificationNames.ProfileUpdated);
    }

    @Override
    public void finalize() throws Throwable {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.MetaSaved);
        nc.removeObserver(this, NotificationNames.ProfileUpdated);
        super.finalize();
    }

    private final Map<ID, List<ReliableMessage>> incomingMessages = new HashMap<>();
    private final ReadWriteLock incomingMessageLock = new ReentrantReadWriteLock();

    @Override
    public void onReceiveNotification(Notification notification) {
        Messenger messenger = Messenger.getInstance();
        Facebook facebook = Facebook.getInstance();
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MetaSaved) || name.equals(NotificationNames.ProfileUpdated)) {
            ID entity = (ID) info.get("ID");
            if (ID.isUser(entity)) {
                // check user
                if (facebook.getPublicKeyForEncryption(entity) == null) {
                    Log.error("user not ready yet: " + entity);
                    return;
                }
            } else {
                return;
            }

            // purge incoming messages waiting for this ID's meta
            ReliableMessage rMsg;
            byte[] response;
            while ((rMsg = getIncomingMessage(entity)) != null) {
                rMsg = messenger.process(rMsg);
                if (rMsg == null) {
                    continue;
                }
                response = messenger.serializeMessage(rMsg);
                if (response != null && response.length > 0) {
                    messenger.sendPackage(response, null, StarShip.SLOWER);
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
            KeyStore keyStore = KeyStore.getInstance();
            SymmetricKey key = keyStore.getCipherKey(me, group, false);
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
    public void suspendMessage(ReliableMessage rMsg) {
        // save this message in a queue waiting sender's meta response
        ID waiting = (ID) rMsg.get("waiting");
        if (waiting == null) {
            waiting = rMsg.getSender();
        } else {
            rMsg.remove("waiting");
        }
        addIncomingMessage(rMsg, waiting);
    }

    @Override
    public void suspendMessage(InstantMessage iMsg) {
        // TODO: save this message in a queue waiting receiver's meta response
    }
}
