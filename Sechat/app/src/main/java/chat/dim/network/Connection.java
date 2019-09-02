package chat.dim.network;

import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

import chat.dim.client.Facebook;
import chat.dim.client.Messanger;
import chat.dim.core.Callback;
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
import chat.dim.protocol.command.HandshakeCommand;
import chat.dim.protocol.command.MetaCommand;
import chat.dim.protocol.command.ProfileCommand;
import chat.dim.stargate.StarStatus;
import chat.dim.utils.Log;

public class Connection {

    String session = null;
    public final Server server;

    public Connection(Server station) {
        server = station;
        station.fsm.connection = new WeakReference<>(this);
    }

    /**
     *  Pack and send message content to receiver
     *
     * @param content - message content
     * @param receiver - contact/group ID
     * @return InstantMessage been sent
     */
    public InstantMessage sendContent(Content content, ID receiver) {
        LocalUser user = server.currentUser;
        if (user == null) {
            // TODO: save the message content in waiting queue
            return null;
        }
        if (Facebook.getInstance().getMeta(receiver) == null) {
            // cannot get public key for receiver
            // TODO: save the message content in waiting queue
            return queryMeta(receiver);
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
        try {
            if (Messanger.getInstance().sendMessage(iMsg, callback, true)) {
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
    public InstantMessage sendCommand(Command cmd) {
        if (server == null) {
            // TODO: save the command in wating queue
            return null;
        }
        return sendContent(cmd, server.identifier);
    }

    //---- urgent command

    public void handshake(String newSession) {
        // TODO: check FSM state == 'Handshaking'

        // TODO: check star status == 'Connected'
        if (server.star == null || server.star.getStatus() != StarStatus.Connected) {
            // FIXME: sometimes the connection will be lost while handshaking
        }
        LocalUser user = SocialNetworkDatabase.getInstance().getCurrentUser();
        if (newSession != null) {
            session = newSession;
        }

        // create handshake command
        HandshakeCommand cmd = new HandshakeCommand(session);
        InstantMessage iMsg = new InstantMessage(cmd, user.identifier, server.identifier);
        ReliableMessage rMsg = null;
        try {
            rMsg = Messanger.getInstance().encryptAndSignMessage(iMsg);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if (rMsg == null) {
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }
        if (cmd.state == HandshakeCommand.START) {
            rMsg.setMeta(user.getMeta());
        }
        // send out directly
        String json = JSON.encode(rMsg);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        server.star.send(data);
    }

    public void handshakeAccepted(String newSession, boolean success) {
        // TODO: check FSM state == 'Handshaking'

        if (success) {
            LocalUser user = SocialNetworkDatabase.getInstance().getCurrentUser();
            server.currentUser = user;
            Log.info("handshake accepted for user: " + user);
            // broadcast profile to DIM network
            postProfile(user.getProfile());
        } else {
            // new session key from station
            session = newSession;
            Log.info("handshake again with session: " + newSession);
        }
    }

    //-------- commands

    public InstantMessage postProfile(Profile profile) {
        return postProfile(profile, null);
    }

    public InstantMessage postProfile(Profile profile, Meta meta) {
        ID identifier = profile.identifier;
        return sendCommand(new ProfileCommand(identifier, meta, profile));
    }

    public InstantMessage queryMeta(ID identifier) {
        return sendCommand(new MetaCommand(identifier));
    }

    public InstantMessage queryProfile(ID identifier) {
        return sendCommand(new ProfileCommand(identifier));
    }

    public InstantMessage queryOnlineUsers() {
        return sendCommand(new Command("users"));
    }

    public InstantMessage searchUsers(String keywords) {
        Command cmd = new Command("search");
        cmd.put("keywords", keywords);
        return sendCommand(cmd);
    }
}
