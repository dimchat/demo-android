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
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.dkd.SecureMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.Group;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.protocol.Command;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.utils.Log;

public class Terminal implements StationDelegate {

    private Facebook facebook = Facebook.getInstance();
    private Messenger messenger = Messenger.getInstance();

    private CommandProcessor processor;
    private ContentDeliver deliver;

    private Server currentServer = null;

    private List<LocalUser> users = null;

    public Terminal() {
        super();
    }

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

    protected Server getCurrentServer() {
        return currentServer;
    }

    protected void setCurrentServer(Server server) {
        currentServer = server;
        deliver = new ContentDeliver(server);
        processor = new CommandProcessor(server, deliver);
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

    //---- Content/processor and deliver

    private boolean processCommand(Command cmd, ID sender) {
        return processor.process(cmd, sender);
    }

    private void sendContent(Content content, ID receiver) {
        deliver.sendContent(content, receiver);
    }

    protected void sendCommand(Command cmd) {
        deliver.sendCommand(cmd);
    }

    public void queryMeta(ID identifier) {
        deliver.queryMeta(identifier);
    }

    protected void login(LocalUser user) {
        deliver.login(user);
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
            boolean needToStore = processCommand(cmd, sender);
            if (!needToStore) {
                Log.info("command processed, drop it: " + content);
                return;
            }
            // other commands
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
