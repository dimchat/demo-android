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
package chat.dim.network;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.database.GroupTable;
import chat.dim.database.SocialNetworkDatabase;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.protocol.Command;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.command.HandshakeCommand;
import chat.dim.protocol.command.MetaCommand;
import chat.dim.protocol.command.ProfileCommand;
import chat.dim.stargate.StarStatus;

public class Terminal implements StationDelegate {

    protected Connection connection = null;

    public String getUserAgent() {
        return "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) " +
                "DIMCoreKit/1.0 (Terminal, like WeChat) " +
                "DIM-by-GSP/1.0.1";
    }

    public String getLanguage() {
        return "zh-CN";
    }

    public boolean login() {
        if (connection == null || connection.server.getStatus() != StarStatus.Connected) {
            // not connect yet
            return false;
        }
        LocalUser user = SocialNetworkDatabase.getInstance().getCurrentUser();
        if (user == null) {
            // user not found
            return false;
        } else if (user.equals(connection.server.currentUser)) {
            // user not change
            return true;
        }

        // switch user,session and handshake again
        connection.server.currentUser = null;
        connection.session = null;
        connection.handshake(null);
        return true;
    }

    //---- Response

    public void processHandshakeCommand(HandshakeCommand cmd) {
        int state = cmd.state;
        if (state == HandshakeCommand.SUCCESS) {
            // handshake OK
            connection.handshakeAccepted(null, true);
        } else if (state == HandshakeCommand.AGAIN) {
            // update session and handshake again
            connection.handshake(cmd.sessionKey);
        } else {
            // handshake rejected
            connection.handshakeAccepted(null, false);
        }
    }

    public void processMetaCommand(MetaCommand cmd) {
        // check meta
        ID identifier = cmd.identifier;
        Meta meta = cmd.meta;
        if (meta == null) {
            return;
        }
        if (!meta.matches(identifier)) {
            throw new IllegalArgumentException("meta error: " + meta);
        }
        // got new meta
        SocialNetworkDatabase userDB = SocialNetworkDatabase.getInstance();
        userDB.saveMeta(meta, identifier);
    }

    public void processProfileCommand(ProfileCommand cmd) {
        // check meta
        processMetaCommand(cmd);

        // check profile
        ID identifier = cmd.identifier;
        Profile profile = cmd.profile;
        if (profile == null) {
            return;
        }
        if (!profile.identifier.equals(identifier)) {
            throw new IllegalArgumentException("profile error: " + profile);
        }
        // got new profile
        // TODO: postNotification("ProfileUpdated")
    }

    public void processOnlineUsersCommand(Command cmd) {
        List users = (List) cmd.get("users");
        // TODO: postNotification("OnlineUsersUpdated");
    }

    public void processSearchUsersCommand(Command cmd) {
        List users = (List) cmd.get("users");
        Map results = (Map) cmd.get("results");
        // TODO: postNotification("SearchUsersUpdated")
    }

    //---- StationDelegate

    @Override
    public void didReceivePackage(byte[] data, Station server) {
        // 1. decode
        String json = new String(data, Charset.forName("UTF-8"));
        ReliableMessage rMsg = ReliableMessage.getInstance(JSON.decode(json));
        if (rMsg == null) {
            // failed to decode reliable message
            return;
        }

        // 2. check sender
        Facebook facebook = Facebook.getInstance();
        ID sender = facebook.getID(rMsg.envelope.sender);
        Meta meta = facebook.getMeta(sender);
        if (meta == null) {
            try {
                meta = Meta.getInstance(rMsg.getMeta());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            if (meta == null) {
                // TODO: query meta from network
                return;
            }
        }

        // 3. check receiver
        ID receiver = facebook.getID(rMsg.envelope.receiver);
        List<ID> users = SocialNetworkDatabase.getInstance().allUsers();
        LocalUser user = null;
        if (receiver.getType().isGroup()) {
            // group message, check group membership
            for (ID item : users) {
                if (GroupTable.existsMember(item, receiver)) {
                    // got group message for this user
                    user = (LocalUser) facebook.getUser(item);
                    break;
                }
            }
            if (user != null) {
                // trim for current user
                rMsg = (ReliableMessage) rMsg.trim(user.identifier);
            }
        } else {
            for (ID item : users) {
                if (item.equals(receiver)) {
                    // got personal message for this user
                    user = (LocalUser) facebook.getUser(item);
                    break;
                }
            }
        }
        if (user == null) {
            // wrong recipient
            return;
        }

        // 4. trans to instant message
        InstantMessage iMsg = Messenger.getInstance().verifyAndDecryptMessage(rMsg);
        if (iMsg == null) {
            // failed to verify/decrypt message
            return;
        }

        // 5. process commands
        Content content = iMsg.content;
        if (content instanceof HistoryCommand) {
            ID group = facebook.getID(content.getGroup());
            if (group != null) {
                // TODO: check group command
                return;
            }
            // NOTE: let the message processor to do the job
            //return;
        } else if (content instanceof Command) {
            Command cmd = (Command) content;
            if (cmd.command.equalsIgnoreCase(Command.HANDSHAKE)) {
                // handshake
                processHandshakeCommand((HandshakeCommand) cmd);
                return;
            } else if (cmd.command.equalsIgnoreCase(Command.META)) {
                // query meta response
                processMetaCommand((MetaCommand) cmd);
                return;
            } else if (cmd.command.equalsIgnoreCase(Command.PROFILE)) {
                // query profile response
                processProfileCommand((ProfileCommand) cmd);
                return;
            } else if (cmd.command.equalsIgnoreCase("users")) {
                // query online users response
                processOnlineUsersCommand(cmd);
                return;
            } else if (cmd.command.equalsIgnoreCase("search")) {
                // search users response
                processSearchUsersCommand(cmd);
                return;
            } else if (cmd.command.equalsIgnoreCase(Command.RECEIPT)) {
                // receipt
                Amanuensis clerk = Amanuensis.getInstance();
                Conversation chatBox = clerk.getConversation(iMsg);
                if (chatBox == null) {
                    throw new NullPointerException("failed to get conversation: " + iMsg);
                }
                if (chatBox.saveReceipt(iMsg)) {
                    // Log: target message state updated with receipt:
                }
                return;
            }
            // NOTE: let the message processor to do the job
            //return;
        }
        /*
        if (sender.getType().isStation()) {
            // ignore station
            return;
        }
        */

        // check meta for new group ID
        Object group = iMsg.getGroup();
        if (group != null) {
            ID gid = facebook.getID(group);
            if (!gid.isBroadcast()) {
                // check meta
                meta = facebook.getMeta(gid);
                if (meta == null) {
                    // NOTICE: if meta for group not found,
                    //         the client will query it automatically
                    // TODO: insert the message to a temporary queue to waiting meta
                    return;
                }
            }
        }

        // normal message, let the clerk to deliver it
        Amanuensis clerk = Amanuensis.getInstance();
        Conversation chatBox = clerk.getConversation(iMsg);
        if (chatBox == null) {
            throw new NullPointerException("failed to get conversation: " + iMsg);
        }
        chatBox.insertMessage(iMsg);
    }

    @Override
    public void didSendPackage(byte[] data, Station server) {
        // TODO: mark it sent
    }

    @Override
    public void didFailToSendPackage(Error error, byte[] data, Station server) {
        // TODO: resend it
    }
}
