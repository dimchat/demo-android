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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.CompletionHandler;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.MessengerDelegate;
import chat.dim.ReliableMessage;
import chat.dim.SecureMessage;
import chat.dim.User;
import chat.dim.crypto.SymmetricKey;
import chat.dim.filesys.ExternalStorage;
import chat.dim.fsm.Machine;
import chat.dim.fsm.State;
import chat.dim.fsm.StateDelegate;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Messenger;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.stargate.wormhole.Hole;
import chat.dim.utils.Log;

public class Server extends Station implements MessengerDelegate, StarDelegate, StateDelegate {

    public final String name;

    private User currentUser = null;
    public String session = null;

    private final StateMachine fsm;

    private Star star = null;
    final private ReadWriteLock starLock = new ReentrantReadWriteLock();

    private Map<String, Object> startOptions = null;

    StationDelegate delegate;

    Server(ID identifier, String host, int port, String title) {
        super(identifier, host, port);
        name = title;
        // connection state machine
        fsm = new StateMachine();
        fsm.server = this;
        fsm.delegate = this;
        fsm.start();
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
        session = null;
    }

    private ServerState getCurrentState() {
        return (ServerState) fsm.getCurrentState();
    }

    StarStatus getStatus() {
        StarStatus status;
        Lock readLock = starLock.readLock();
        readLock.lock();
        try {
            if (star == null) {
                status = StarStatus.Error;
            } else {
                status = star.getStatus();
            }
        } finally {
            readLock.unlock();
        }
        return status;
    }

    //---- urgent command for connection

    private static void setLastReceivedMessageTime(HandshakeCommand cmd) {
        ConversationDatabase db = ConversationDatabase.getInstance();
        InstantMessage iMsg = db.lastReceivedMessage();
        if (iMsg != null) {
            Date lastTime = iMsg.envelope.getTime();
            if (lastTime != null) {
                long timestamp = lastTime.getTime() / 1000;
                cmd.put("last_time", timestamp);
            }
        }
    }

    public void handshake(String newSession) {
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!StateMachine.handshakingState.equals(state.name)) {
            // FIXME: sometimes the connection state will be reset
            return;
        }
        // check connection status == 'Connected'
        if (getStatus() != StarStatus.Connected) {
            // FIXME: sometimes the connection will be lost while handshaking
            return;
        }
        session = newSession;
        // create handshake command
        HandshakeCommand cmd = new HandshakeCommand(session);
        setLastReceivedMessageTime(cmd);
        InstantMessage<ID, SymmetricKey> iMsg = new InstantMessage<>(cmd, currentUser.identifier, identifier);
        Messenger messenger = Messenger.getInstance();
        SecureMessage<ID, SymmetricKey> sMsg = messenger.encryptMessage(iMsg);
        ReliableMessage<ID, SymmetricKey> rMsg = messenger.signMessage(sMsg);
        if (rMsg == null) {
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }
        // first handshake?
        if (cmd.state == HandshakeCommand.HandshakeState.START) {
            // [Meta protocol]
            rMsg.setMeta(currentUser.getMeta());
        }
        // send out directly
        byte[] data = messenger.serializeMessage(rMsg);
        send(data);
    }

    public void handshakeAccepted() {
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!state.name.equals(StateMachine.handshakingState)) {
            // FIXME: sometimes the connection state will be reset
            Log.error("server state error: " + state.name);
        }
        Log.info("handshake accepted for user: " + currentUser);
        // call client
        delegate.onHandshakeAccepted(session, this);
    }

    public void handshakeAgain(String sessionKey) {
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!state.name.equals(StateMachine.handshakingState)) {
            // FIXME: sometimes the connection state will be reset
            Log.error("server state error: " + state.name);
        }
        // new session key from station
        Log.info("handshake again with session: " + sessionKey);
        handshake(sessionKey);
    }

    //--------

    void start(Map<String, Object> options) {

        Messenger messenger = Messenger.getInstance();
        messenger.setDelegate(this);

        // fsm.changeState(fsm.defaultStateName);

        if (options == null) {
            options = new HashMap<>();
            options.put("host", getHost());
            options.put("port", getPort());
        } else {
            if (options.get("host") == null) {
                options.put("host", getHost());
            }
            if (options.get("port") == null) {
                options.put("port", getPort());
            }
        }
        startOptions = options;

        Lock writeLock = starLock.writeLock();
        writeLock.lock();
        try {
            if (star == null) {
                star = new Hole(this);
            }

            // TODO: post notification "StationConnecting"

            star.launch(options);
        } finally {
            writeLock.unlock();
        }

        // TODO: let the subclass to create StarGate
    }

    void end() {
        Lock readLock = starLock.readLock();
        readLock.lock();
        try {
            if (star != null) {
                star.terminate();
            }
        } finally {
            readLock.unlock();
        }

        fsm.stop();
    }

    void pause() {
        Lock readLock = starLock.readLock();
        readLock.lock();
        try {
            if (star != null) {
                star.enterBackground();
            }
        } finally {
            readLock.unlock();
        }

        fsm.pause();
    }

    void resume() {
        Lock readLock = starLock.readLock();
        readLock.lock();
        try {
            if (star != null) {
                star.enterForeground();
            }
        } finally {
            readLock.unlock();
        }

        fsm.resume();
    }

    private void send(byte[] payload) {
        Lock readLock = starLock.readLock();
        readLock.lock();
        try {
            if (star != null) {
                star.send(payload);
            }
        } finally {
            readLock.unlock();
        }
    }

    private void restart() {
        if (startOptions == null) {
            return;
        }

        Lock writeLock = starLock.writeLock();
        writeLock.lock();
        try {
            if (star instanceof Hole) {
                ((Hole) star).disconnect();
                ((Hole) star).close();
            }
            star = new Hole(this);
            star.launch(startOptions);
        } finally {
            writeLock.unlock();
        }
    }

    //-------- StarDelegate

    @Override
    public void onReceive(byte[] responseData, Star star) {
        Log.info("received " + responseData.length + " bytes");
        delegate.onReceivePackage(responseData, this);
    }

    @Override
    public void onStatusChanged(StarStatus status, Star star) {
        Log.info("status changed: " + status);
        fsm.tick();
    }

    @Override
    public void onFinishSend(byte[] requestData, Error error, Star star) {
        Log.info("sent " + requestData.length + " bytes");
        CompletionHandler handler = null;

        String key = RequestWrapper.getKey(requestData);
        RequestWrapper wrapper = sendingTable.get(key);
        if (wrapper != null) {
            handler = wrapper.handler;
            sendingTable.remove(key);
        }

        if (error == null) {
            // send success
            delegate.didSendPackage(requestData, this);
        } else {
            delegate.didFailToSendPackage(error, requestData, this);
        }

        if (handler != null) {
            // tell the handler to do the resending job
            if (error == null) {
                handler.onSuccess();
            } else {
                handler.onFailed(error);
            }
        }
    }

    //---- MessengerDelegate

    private List<RequestWrapper> waitingList = new ArrayList<>();
    private Map<String, RequestWrapper> sendingTable = new HashMap<>();

    private void sendAllWaiting() {
        RequestWrapper wrapper;
        ServerState state;
        while (waitingList.size() > 0 && getStatus() == StarStatus.Connected) {
            state = getCurrentState();
            if (state == null || !state.name.equals(StateMachine.runningState)) {
                break;
            }
            wrapper = waitingList.remove(0);
            send(wrapper);
        }
    }

    private void send(RequestWrapper wrapper) {
        send(wrapper.data);

        if (wrapper.handler != null) {
            String key = RequestWrapper.getKey(wrapper.data);
            sendingTable.put(key, wrapper);
        }
    }

    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        RequestWrapper wrapper = new RequestWrapper(data, handler);

        ServerState state = getCurrentState();
        if (!state.name.equals(StateMachine.runningState)) {
            waitingList.add(wrapper);
            return true;
        }

        send(wrapper);
        return true;
    }

    @Override
    public String uploadData(byte[] data, InstantMessage iMsg) {
        ID sender = ID.getInstance(iMsg.envelope.getSender());
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
    public void enterState(State state, Machine machine) {
        ServerState serverState = (ServerState) state;

        Map<String, Object> info = new HashMap<>();
        info.put("state", serverState.name);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.ServerStateChanged, this, info);

        switch (serverState.name) {
            case StateMachine.handshakingState: {
                // start handshake
                String session = this.session;
                this.session = null;
                handshake(session);
                break;
            }
            case StateMachine.runningState: {
                // send all packages waiting
                sendAllWaiting();
                break;
            }
            case StateMachine.errorState: {
                // reconnect
                restart();
                break;
            }
        }
    }

    @Override
    public void exitState(State state, Machine machine) {

    }

    @Override
    public void pauseState(State state, Machine machine) {

    }

    @Override
    public void resumeState(State state, Machine machine) {

    }
}
