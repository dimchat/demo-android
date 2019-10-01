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
import java.util.ArrayList;
import java.util.List;

import chat.dim.common.Amanuensis;
import chat.dim.common.Facebook;
import chat.dim.common.Messenger;
import chat.dim.core.Callback;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.dkd.SecureMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.Group;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.protocol.Command;
import chat.dim.protocol.command.MetaCommand;
import chat.dim.protocol.command.ProfileCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.utils.Log;

public class Terminal implements StationDelegate {

    private Facebook facebook = Facebook.getInstance();
    private Messenger messenger = Messenger.getInstance();

    private CommandProcessor processor = new CommandProcessor();

    public Server currentServer = null;

    private List<LocalUser> users = null;

    /**
     *  format: "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) DIMCoreKit/1.0 (Terminal, like WeChat) DIM-by-GSP/1.0.1"
     */
    public String getUserAgent() {
        return "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) " +
                "DIMCoreKit/1.0 (Terminal, like WeChat) " +
                "DIM-by-GSP/1.0.1";
    }

    public String getLanguage() {
        return "zh-CN";
    }

    public LocalUser getCurrentUser() {
        return currentServer == null ? null : currentServer.getCurrentUser();
    }

    private void setCurrentUser(LocalUser user) {
        if (currentServer != null) {
            currentServer.setCurrentUser(user);
        }
        // TODO: update users list
    }

    public List<LocalUser> allUsers() {
        if (users == null) {
            users = new ArrayList<>();
            List<ID> list = facebook.database.allUsers();
            LocalUser user;
            for (ID item : list) {
                user = (LocalUser) facebook.getUser(item);
                if (user == null) {
                    throw new NullPointerException("failed to get local user: " + item);
                }
                users.add(user);
            }
        }
        return users;
    }

    //---- Request

    /**
     *  Pack and send message content to receiver
     *
     * @param content - message content
     * @param receiver - contact/group ID
     * @return InstantMessage been sent
     */
    public InstantMessage sendContent(Content content, ID receiver) {
        LocalUser user = getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        Facebook facebook = Facebook.getInstance();
        if (!receiver.isBroadcast() && facebook.getMeta(receiver) == null) {
            // NOTICE: if meta for sender not found,
            //         the client will query it automatically
            // TODO: save the message content in waiting queue
            return null;
        }
        // make instant message
        InstantMessage iMsg = new InstantMessage(content, user.identifier, receiver);
        // callback
        Callback callback = new Callback() {
            @Override
            public void onFinished(Object result, Error error) {
                String event;
                if (error == null) {
                    event = "MessageSent";
                    //iMsg.state = Accepted;
                } else {
                    event = "SendMessageFailed";
                    //iMsg.state = Error;
                    //iMsg.error = error;
                }
                // TODO: post notification with event name and message
            }
        };
        // send out
        Messenger messenger = Messenger.getInstance();
        if (messenger.sendMessage(iMsg, callback, true)) {
            return iMsg;
        }
        // error
        return null;
    }

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return InstantMessage been sent
     */
    public InstantMessage sendCommand(Command cmd) {
        if (currentServer == null) {
            throw new IllegalStateException("not connect yet");
        }
        return sendContent(cmd, currentServer.identifier);
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     * @return InstantMessage been sent
     */
    public InstantMessage broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        return sendContent(content, ID.ANYONE);
    }

    //-------- commands

    public void broadcastProfile(Profile profile) {
        LocalUser user = getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        assert profile.identifier.equals(user.identifier);
        // pack and send profile to every contact
        Command cmd = new ProfileCommand(profile.identifier, profile);
        List<ID> contacts = user.getContacts();
        for (ID contact : contacts) {
            sendContent(cmd, contact);
        }
    }

    public InstantMessage postProfile(Profile profile) {
        return postProfile(profile, null);
    }

    public InstantMessage postProfile(Profile profile, Meta meta) {
        Command cmd = new ProfileCommand(profile.identifier, meta, profile);
        return sendCommand(cmd);
    }

    public void postContacts(List<ID> contacts) {
        // TODO: encrypt contacts and send to station
    }

    public InstantMessage queryMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            return null;
        }
        Command cmd = new MetaCommand(identifier);
        return sendCommand(cmd);
    }

    public InstantMessage queryProfile(ID identifier) {
        if (identifier.isBroadcast()) {
            return null;
        }
        Command cmd = new ProfileCommand(identifier);
        return sendCommand(cmd);
    }

    public InstantMessage queryOnlineUsers() {
        Command cmd = new Command("users");
        return sendCommand(cmd);
    }

    public InstantMessage searchUsers(String keywords) {
        Command cmd = new Command("search");
        cmd.put("keywords", keywords);
        return sendCommand(cmd);
    }

    public boolean login(LocalUser user) {
        if (currentServer == null) {
            throw new NullPointerException("not connect yet");
        }
        if (user == null) {
            user = facebook.database.getCurrentUser();
            if (user == null) {
                // user not found
                return false;
            }
        }
        if (user.equals(getCurrentUser())) {
            // user not change
            return true;
        }
        // clear session
        currentServer.session = null;

        setCurrentUser(user);

        currentServer.handshake(null);
        return true;
    }

    private boolean processCommand(Command cmd, ID sender) {
        if (processor.server == null) {
            processor.server = currentServer;
        }
        return processor.process(cmd, sender);
    }

    //---- StationDelegate

    @Override
    public void didReceivePackage(byte[] data, Station server) {
        // 1. decode to reliable message
        String json = new String(data, Charset.forName("UTF-8"));
        ReliableMessage rMsg = ReliableMessage.getInstance(JSON.decode(json));
        if (rMsg == null) {
            // failed to decode reliable message
            throw new NullPointerException("failed to decode message: " + json);
        }

        // 2. verify it with sender's meta.key
        SecureMessage sMsg = messenger.verifyMessage(rMsg);
        if (sMsg == null) {
            // NOTICE: if meta for sender not found,
            //         the client will query it automatically
            // TODO: insert the message to a temporary queue to waiting meta
            return;
        }

        // 3. check receiver
        LocalUser user = null;
        ID receiver = facebook.getID(rMsg.envelope.receiver);
        List<LocalUser> users = allUsers();
        if (receiver.getType().isGroup()) {
            // group message, check group membership
            for (LocalUser item : users) {
                if (facebook.existsMember(item.identifier, receiver)) {
                    // got group message for this user
                    user = item;
                    break;
                }
            }
            if (user != null) {
                // trim for current user
                sMsg = sMsg.trim(user.identifier);
            }
        } else {
            for (LocalUser item : users) {
                if (item.identifier.equals(receiver)) {
                    // got personal message for this user
                    user = item;
                    break;
                }
            }
        }
        if (user == null) {
            // wrong recipient
            return;
        }

        // 4. decrypt it for local user
        InstantMessage iMsg = messenger.decryptMessage(sMsg);
        if (iMsg == null) {
            // failed to decrypt message
            return;
        }
        ID sender = facebook.getID(iMsg.envelope.sender);
        Content content = iMsg.content;

        // check meta for new group ID
        ID gid = facebook.getID(content.getGroup());
        if (gid != null) {
            if (!gid.isBroadcast()) {
                // check meta
                Meta meta = facebook.getMeta(gid);
                if (meta == null) {
                    // NOTICE: if meta for group not found,
                    //         the client will query it automatically
                    // TODO: insert the message to a temporary queue to waiting meta
                    return;
                }
            }
            // check whether the group members info needs update
            Group group = facebook.getGroup(gid);
            // if the group info not found, and this is not an 'invite' command
            //     query group info from the sender
            boolean needsUpdate = group.getFounder() == null;
            if (content instanceof InviteCommand) {
                // FIXME: can we trust this stranger?
                //        may be we should keep this members list temporary,
                //        and send 'query' to the founder immediately.
                // TODO: check whether the members list is a full list,
                //       it should contain the group owner(founder)
                needsUpdate = false;
            }
            if (needsUpdate) {
                QueryCommand query = new QueryCommand(gid);
                sendContent(query, sender);
            }
        }

        Amanuensis clerk = Amanuensis.getInstance();

        // 5. process commands
        if (content instanceof Command) {
            Command cmd = (Command) content;
            if (!processCommand(cmd, sender)) {
                Log.info("command processed: " + content);
                return;
            }
            String command = cmd.command;
            if (command.equalsIgnoreCase(Command.RECEIPT)) {
                // receipt
                if (clerk.saveReceipt(iMsg)) {
                    Log.info("target message state updated with receipt: " + cmd);
                }
                return;
            }
            // NOTE: let the message processor to do the job
            //return;
        }

        if (sender.getType().isStation()) {
            Log.info("*** message from station: " + content);
            //return;
        }

        // normal message, let the clerk to deliver it
        clerk.saveMessage(iMsg);
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
