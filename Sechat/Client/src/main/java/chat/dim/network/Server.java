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

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import chat.dim.User;
import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.common.MessageTransmitter;
import chat.dim.filesys.ExternalStorage;
import chat.dim.fsm.BaseTransition;
import chat.dim.fsm.Delegate;
import chat.dim.model.ConversationDatabase;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Command;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.startrek.Gate;
import chat.dim.startrek.Ship;
import chat.dim.startrek.StarShip;
import chat.dim.utils.Log;

public class Server extends Station implements Messenger.Delegate, Delegate<StateMachine, BaseTransition<StateMachine>, ServerState> {

    private User currentUser = null;

    private boolean paused = false;

    private String sessionKey = null;
    private final Session session;

    public final String name;

    private final StateMachine fsm;

    private WeakReference<ServerDelegate> delegateRef = null;

    Server(ID identifier, String host, int port, String title) {
        super(identifier, host, port);
        session = new Session(host, port, Messenger.getInstance());
        name = title;
        // connection state machine
        fsm = new StateMachine(this);
        fsm.start();
    }

    public ServerDelegate getDelegate() {
        if (delegateRef == null) {
            return null;
        }
        return delegateRef.get();
    }

    public void setDelegate(ServerDelegate delegate) {
        delegateRef = new WeakReference<>(delegate);
    }

    public User getCurrentUser() {
        return currentUser;
    }
    public void setCurrentUser(User user) {
        if (user.equals(currentUser)) {
            return;
        }
        currentUser = user;
        // switch state for re-login
        fsm.setSessionKey(null);
    }

    private ServerState getCurrentState() {
        return fsm.getCurrentState();
    }

    Gate.Status getStatus() {
        return session.gate.getStatus();
    }

    private ReliableMessage packCommand(Command cmd) {
        if (currentUser == null) {
            throw new NullPointerException("current user not set");
        }

        Facebook facebook = Facebook.getInstance();
        if (facebook.getPublicKeyForEncryption(identifier) == null) {
            cmd.setGroup(ID.EVERYONE);
        }

        Envelope env = Envelope.create(currentUser.identifier, identifier, null);
        InstantMessage iMsg = InstantMessage.create(env, cmd);
        Messenger messenger = Messenger.getInstance();
        SecureMessage sMsg = messenger.encryptMessage(iMsg);
        if (sMsg == null) {
            throw new NullPointerException("failed to encrypt message: " + iMsg.getMap());
        }
        ReliableMessage rMsg = messenger.signMessage(sMsg);
        if (rMsg == null) {
            throw new NullPointerException("failed to sign message: " + sMsg.getMap());
        }
        return rMsg;
    }

    //---- urgent command for connection

    private static void setLastReceivedMessageTime(HandshakeCommand cmd) {
        ConversationDatabase db = ConversationDatabase.getInstance();
        InstantMessage iMsg = db.lastReceivedMessage();
        if (iMsg != null) {
            Date lastTime = iMsg.getTime();
            if (lastTime != null) {
                long timestamp = lastTime.getTime() / 1000;
                cmd.put("last_time", timestamp);
            }
        }
    }

    public void handshake(String newSessionKey) {
        if (currentUser == null) {
            // current user not set yet
            return;
        }
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!state.equals(ServerState.CONNECTED) && !state.equals(ServerState.HANDSHAKING)) {
            // FIXME: sometimes the connection state will be reset
            Log.error("server state not for handshaking: " + state.name);
            return;
        }
        // check connection status == 'Connected'
        if (getStatus() != Gate.Status.CONNECTED) {
            // FIXME: sometimes the connection will be lost while handshaking
            Log.error("server not connected");
            return;
        }

        if (newSessionKey != null) {
            sessionKey = newSessionKey;
        }
        fsm.setSessionKey(null);

        // create handshake command
        HandshakeCommand cmd = new HandshakeCommand(sessionKey);
        setLastReceivedMessageTime(cmd);
        ReliableMessage rMsg = packCommand(cmd);
        // first handshake?
        if (cmd.state == HandshakeCommand.HandshakeState.START) {
            // [Meta/Visa protocol]
            rMsg.setMeta(currentUser.getMeta());
            rMsg.setVisa(currentUser.getVisa());
        }
        // send out directly
        Messenger messenger = Messenger.getInstance();
        byte[] data = messenger.serializeMessage(rMsg);
        // Urgent Command
        session.send(data, StarShip.URGENT, null);
    }

    public void handshakeAccepted() {
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!state.equals(ServerState.HANDSHAKING)) {
            // FIXME: sometimes the connection state will be reset
            Log.error("server state not handshaking: " + state.name);
        }
        Log.info("handshake accepted for user: " + currentUser);

        fsm.setSessionKey(sessionKey);

        // call client
        getDelegate().onHandshakeAccepted(sessionKey, this);
    }

    //--------

    void start(Map<String, Object> options) {

        Messenger messenger = Messenger.getInstance();
        messenger.setDelegate(this);

        // fsm.changeState(fsm.defaultStateName);

        if (!session.isRunning()) {
            // TODO: post notification "StationConnecting"
            session.start();
        }

        // TODO: let the subclass to create StarGate
    }

    void end() {
        if (session.isRunning()) {
            session.close();
        }

        fsm.stop();
    }

    void pause() {
        if (paused) {
            return;
        }

        fsm.pause();

        paused = true;
    }

    void resume() {
        if (!paused) {
            return;
        }
        paused = false;

        fsm.resume();
    }

//    //-------- Gate Delegate
//
//    @Override
//    public byte[] onReceived(Gate gate, Ship ship) {
//        byte[] payload = ship.getPayload();
//        Log.info("received " + payload.length + " bytes");
//        getDelegate().onReceivePackage(payload, this);
//    }
//
//    @Override
//    public void onStatusChanged(Gate gate, Gate.Status oldStatus, Gate.Status newStatus) {
//        Log.info("status changed: " + oldStatus + " -> " + newStatus);
//        fsm.tick();
//    }

//    @Override
//    public void onSent(StarGate star, Package request, Error error) {
//        Log.info("sent " + request.getSize() + " bytes");
//        Messenger.CompletionHandler handler = null;
//
//        byte[] requestData = request.body.getBytes();
//        String key = RequestWrapper.getKey(requestData);
//        RequestWrapper wrapper = sendingTable.get(key);
//        if (wrapper != null) {
//            handler = wrapper.handler;
//            sendingTable.remove(key);
//        }
//
//        if (error == null) {
//            // send success
//            getDelegate().didSendPackage(requestData, this);
//        } else {
//            getDelegate().didFailToSendPackage(error, requestData, this);
//        }
//
//        if (handler != null) {
//            // tell the handler to do the resending job
//            if (error == null) {
//                handler.onSuccess();
//            } else {
//                handler.onFailed(error);
//            }
//        }
//    }

//    //---- MessengerDelegate
//
//    private List<RequestWrapper> waitingList = new ArrayList<>();
//    private Map<String, RequestWrapper> sendingTable = new HashMap<>();
//
//    private void sendAllWaiting() {
//        RequestWrapper wrapper;
//        ServerState state;
//        while (waitingList.size() > 0 && getStatus() == Gate.Status.Connected) {
//            state = getCurrentState();
//            if (!state.equals(ServerState.RUNNING)) {
//                break;
//            }
//            wrapper = waitingList.remove(0);
//            send(wrapper);
//        }
//    }
//
//    private void send(RequestWrapper wrapper) {
//        send(wrapper.data, wrapper.priority);
//
//        if (wrapper.handler != null) {
//            String key = RequestWrapper.getKey(wrapper.data);
//            sendingTable.put(key, wrapper);
//        }
//    }

    @Override
    public boolean sendPackage(byte[] data, Messenger.CompletionHandler handler, int priority) {
        Ship.Delegate delegate = null;
        if (handler instanceof MessageTransmitter.CompletionHandler) {
            Messenger.Callback callback = ((MessageTransmitter.CompletionHandler) handler).callback;
            if (callback instanceof Ship.Delegate) {
                delegate = (Ship.Delegate) callback;
            }
        }

        if (session.send(data, priority, delegate)) {
            if (handler != null) {
                handler.onSuccess();
            }
            return true;
        } else {
            if (handler != null) {
                handler.onFailed(new Error("Server error: failed to send data package"));
            }
            return false;
        }
    }

    @Override
    public String uploadData(byte[] data, InstantMessage iMsg) {
        ID sender = iMsg.getSender();
        FileContent content = (FileContent) iMsg.getContent();
        String filename = content.getFilename();

        FtpServer ftp = FtpServer.getInstance();
        return ftp.uploadEncryptedData(data, filename, sender);
    }

    @Override
    public byte[] downloadData(String url, InstantMessage iMsg) {

        FtpServer ftp = FtpServer.getInstance();
        String path = ftp.downloadEncryptedData(url);
        if (path == null) {
            return null;
        }
        try {
            return ExternalStorage.loadData(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //-------- StateDelegate

    @Override
    public void enterState(ServerState state, StateMachine machine) {
        Map<String, Object> info = new HashMap<>();
        info.put("state", state.name);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.ServerStateChanged, this, info);

        switch (state.name) {
            case ServerState.HANDSHAKING: {
                // start handshake
                handshake(null);
                break;
            }
            case ServerState.RUNNING: {
                // TODO: send all packages waiting?
                break;
            }
            case ServerState.ERROR: {
                // TODO: reconnect?
                break;
            }
        }
    }

    @Override
    public void exitState(ServerState state, StateMachine machine) {
    }

    @Override
    public void pauseState(ServerState state, StateMachine machine) {
        /* TODO: reuse session key?
        if (ServerState.RUNNING.equals(state.name)) {
            // save old session key for re-login
            oldSession = sessionKey;
        }
         */
    }

    @Override
    public void resumeState(ServerState state, StateMachine machine) {
        if (ServerState.RUNNING.equals(state.name)) {
            // switch state for re-login
            fsm.setSessionKey(null);
        }
    }
}
