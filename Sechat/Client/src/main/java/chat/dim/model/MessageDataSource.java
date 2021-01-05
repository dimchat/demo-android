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
    private final Map<ID, List<InstantMessage>> outgoingMessages = new HashMap<>();

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MetaSaved) || name.equals(NotificationNames.ProfileUpdated)) {
            Facebook facebook = Facebook.getInstance();
            ID entity = (ID) info.get("ID");
            if (entity.isUser()) {
                // check user
                if (facebook.getPublicKeyForEncryption(entity) == null) {
                    Log.error("user not ready yet: " + entity);
                    return;
                }
            }
            Messenger messenger = Messenger.getInstance();

            // processing incoming messages
            List<ReliableMessage> incoming = incomingMessages.remove(entity);
            if (incoming != null) {
                ReliableMessage res;
                for (ReliableMessage item : incoming) {
                    res = messenger.process(item);
                    if (res == null) {
                        continue;
                    }
                    messenger.sendMessage(res, null, StarShip.SLOWER);
                }
            }

            // processing outgoing messages
            List<InstantMessage> outgoing = outgoingMessages.remove(entity);
            if (outgoing != null) {
                for (InstantMessage item : outgoing) {
                    messenger.sendMessage(item, null, StarShip.SLOWER);
                }
            }
        }
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
            waiting = rMsg.getGroup();
            if (waiting == null) {
                waiting = rMsg.getSender();
            }
        } else {
            rMsg.remove("waiting");
        }
        List<ReliableMessage> list = incomingMessages.get(waiting);
        if (list == null) {
            list = new ArrayList<>();
            incomingMessages.put(waiting, list);
        }
        list.add(rMsg);
    }

    @Override
    public void suspendMessage(InstantMessage iMsg) {
        // save this message in a queue waiting receiver's meta response
        ID waiting = (ID) iMsg.get("waiting");
        if (waiting == null) {
            waiting = iMsg.getGroup();
            if (waiting == null) {
                waiting = iMsg.getSender();
            }
        } else {
            iMsg.remove("waiting");
        }
        List<InstantMessage> list = outgoingMessages.get(waiting);
        if (list == null) {
            list = new ArrayList<>();
            outgoingMessages.put(waiting, list);
        }
        list.add(iMsg);
    }
}
