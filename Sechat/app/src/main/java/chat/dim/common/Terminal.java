package chat.dim.common;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.core.Barrack;
import chat.dim.core.Callback;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.format.JSON;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.mkm.entity.Profile;
import chat.dim.network.Station;
import chat.dim.network.StationDelegate;
import chat.dim.protocol.CommandContent;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.command.HandshakeCommand;
import chat.dim.protocol.command.MetaCommand;
import chat.dim.protocol.command.ProfileCommand;

public class Terminal implements StationDelegate {

    protected Server currentStation;
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

    //---- Packing

    /**
     *  Pack and send message content to receiver
     *
     * @param content - message content
     * @param receiver - contact/group ID
     * @return InstantMessage been sent
     */
    public InstantMessage sendContent(Content content, ID receiver) {
        User user = getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            return null;
        }
        if (Facebook.getInstance().getMeta(receiver) == null) {
            // cannot get public key for receiver
            queryMeta(receiver);
            // TODO: save the message content in waiting queue
            return null;
        }
        ID sender = user.identifier;

        // make instant message
        InstantMessage iMsg = new InstantMessage(content, sender, receiver);
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
        Transceiver transceiver = Transceiver.getInstance();
        try {
            if (transceiver.sendMessage(iMsg, callback, true)) {
                return iMsg;
            }
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return InstantMessage been sent
     */
    public InstantMessage sendCommand(CommandContent cmd) {
        if (currentStation == null) {
            // TODO: save the command in wating queue
            return null;
        }
        return sendContent(cmd, currentStation.identifier);
    }

    //---- Request

    public boolean login(User user) {
        if (user == null || user.equals(getCurrentUser())) {
            // user not change
            return false;
        }

        // clear session
        session = null;

        setCurrentUser(user);

        // add to the list of this client
        if (!users.contains(user)) {
            users.add(user);
        }
        return true;
    }

    public void onHandshakeAccepted(String session) {
        // post current profile to station
        Profile profile = getCurrentUser().getProfile();
        if (profile != null) {
            postProfile(profile);
        }
    }

    public InstantMessage postProfile(Profile profile) {
        return postProfile(profile, null);
    }

    public InstantMessage postProfile(Profile profile, Meta meta) {
        ID identifier = getCurrentUser().identifier;
        if (!profile.identifier.equals(identifier)) {
            throw new IllegalArgumentException("profile ID not match: " + identifier);
        }
        return sendCommand(new ProfileCommand(identifier, meta, profile));
    }

    public InstantMessage queryMeta(ID identifier) {
        return sendCommand(new MetaCommand(identifier));
    }

    public InstantMessage queryProfile(ID identifier) {
        return sendCommand(new ProfileCommand(identifier));
    }

    public InstantMessage queryOnlineUsers() {
        return sendCommand(new CommandContent("users"));
    }

    public InstantMessage searchUsers(String keywords) {
        CommandContent cmd = new CommandContent("search");
        cmd.put("keywords", keywords);
        return sendCommand(cmd);
    }

    //---- Response

    public void processHandshakeCommand(HandshakeCommand cmd) {
        int state = cmd.state;
        if (state == HandshakeCommand.SUCCESS) {
            // handshake OK
            currentStation.handshakeAccepted(session, true);
            onHandshakeAccepted(session);
        } else if (state == HandshakeCommand.AGAIN) {
            // update session and handshake again
            session = cmd.sessionKey;
            currentStation.handshake(session);
        } else {
            // handshake rejected
            currentStation.handshakeAccepted(null, false);
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
        Facebook facebook = Facebook.getInstance();
        facebook.saveMeta(meta, identifier);
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

    public void processOnlineUsersCommand(CommandContent cmd) {
        List users = (List) cmd.get("users");
        // TODO: postNotification("OnlineUsersUpdated");
    }

    public void processSearchUsersCommand(CommandContent cmd) {
        List users = (List) cmd.get("users");
        Map results = (Map) cmd.get("results");
        // TODO: postNotification("SearchUsersUpdated")
    }

    //---- StationDelegate

    @Override
    public void didReceivePackage(byte[] data, Station server) {
        Barrack barrack = Facebook.getInstance();
        Transceiver trans = Transceiver.getInstance();

        // 1. decode
        String json = new String(data, Charset.forName("UTF-8"));
        ReliableMessage rMsg = ReliableMessage.getInstance(JSON.decode(json));
        if (rMsg == null) {
            // failed to decode reliable message
            return;
        }

        // 2. check sender
        ID sender = barrack.getID(rMsg.envelope.sender);
        Meta meta = barrack.getMeta(sender);
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
        ID receiver = barrack.getID(rMsg.envelope.receiver);
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
            iMsg = trans.verifyAndDecryptMessage(rMsg);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (iMsg == null) {
            // failed to verify/decrypt message
            return;
        }

        // 5. process commands
        Content content = iMsg.content;
        if (content instanceof HistoryCommand) {
            ID group = barrack.getID(content.getGroup());
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
        Amanuensis clerk = Amanuensis.getInstance();
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
