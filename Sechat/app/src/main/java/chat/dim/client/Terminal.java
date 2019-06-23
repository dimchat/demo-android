package chat.dim.client;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import chat.dim.core.Barrack;
import chat.dim.core.Transceiver;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocol.CommandContent;
import chat.dim.protocol.HistoryCommand;

public class Terminal implements StationDelegate {

    protected Station currentStation;
    protected String session;

    protected List<User> users = new ArrayList<>();

    public List<User> getUsers() {
        return users;
    }

    public User getCurrentUser() {
        return currentStation == null ? null : currentStation.currentUser;
    }

    public void setCurrentUser(User user) {
        currentStation.currentUser = user;
        if (user != null && !users.contains(user)) {
            // insert the user to the fist
            users.add(0, user);
        }
    }

    public void addUser(User user) {
        // check current user
        if (currentStation.currentUser == null) {
            currentStation.currentUser = user;
        }
        // add to list
        if (user != null && !users.contains(user)) {
            users.add(user);
        }
    }

    public void removeUser(User user) {
        // remove from list
        users.remove(user);
        // check current user
        if (user.equals(currentStation.currentUser)) {
            currentStation.currentUser = users.size() > 0 ? users.get(0) : null;
        }
    }

    public String getUserAgent() {
        return "DIMP/1.0 (Linux; U; Android 4.1; zh-CN) " +
                "DIMCoreKit/1.0 (Terminal, like WeChat) " +
                "DIM-by-GSP/1.0.1";
    }

    public String getLanguage() {
        return "zh-CN";
    }

    //---- StationDelegate

    @Override
    public void didReceivePackage(byte[] data, Station server) {
        Barrack barrack = Barrack.getInstance();
        Transceiver trans = Transceiver.getInstance();

        // 1. decode
        String json = new String(data, Charset.forName("UTF-8"));
        ReliableMessage rMsg = ReliableMessage.getInstance(JSON.decode(json));
        if (rMsg == null) {
            // failed to decode reliable message
            return;
        }

        // 2. check sender
        ID sender = ID.getInstance(rMsg.envelope.sender);
        Meta meta = barrack.getMeta(sender);
        if (meta == null) {
            meta = Meta.getInstance(rMsg.getMeta());
            if (meta == null) {
                // TODO: query meta from network
                return;
            }
        }

        // 3. check receiver
        ID receiver = ID.getInstance(rMsg.envelope.receiver);
        User user = null;
        if (receiver.getType().isGroup()) {
            // FIXME: maybe other user?
            user = getCurrentUser();
            receiver = user.identifier;
        } else if (getCurrentUser().identifier.equals(receiver)) {
            user = getCurrentUser();
        } else {
            for (User item : getUsers()) {
                if (item.identifier.equals(receiver)) {
                    // got new message for this user
                    user = item;
                    break;
                }
            }
        }
        if (user == null) {
            // wrong recipient
            return;
        }

        // 4. trans to instant message
        InstantMessage iMsg = null;
        try {
            iMsg = trans.verifyAndDecryptMessage(rMsg, getUsers());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (iMsg == null) {
            // failed to verify/decrypt message
            return;
        }

        // 5. process commands
        Content content = iMsg.content;
        if (content instanceof HistoryCommand) {
            ID group = ID.getInstance(content.getGroup());
            if (group != null) {
                // TODO: check group command
                return;
            }
            // NOTE: let the message processor to do the job
            //return;
        } else if (content instanceof CommandContent) {
            CommandContent cmd = (CommandContent) content;
            if (cmd.command.equalsIgnoreCase(CommandContent.HANDSHAKE)) {
                // TODO: handshake
                return;
            } else if (cmd.command.equalsIgnoreCase(CommandContent.META)) {
                // TODO: query meta response
                return;
            } else if (cmd.command.equalsIgnoreCase(CommandContent.PROFILE)) {
                // TODO: query profile response
                return;
            } else if (cmd.command.equalsIgnoreCase("users")) {
                // TODO: query online users response
                return;
            } else if (cmd.command.equalsIgnoreCase("search")) {
                // TODO: search users response
                return;
            } else if (cmd.command.equalsIgnoreCase(CommandContent.RECEIPT)) {
                // TODO: receipt
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

        // normal message, let the clerk to deliver it

    }

    @Override
    public void didSendPackage(byte[] data, Station server) {

    }

    @Override
    public void didFailToSendPackage(Error error, byte[] data, Station server) {

    }
}
