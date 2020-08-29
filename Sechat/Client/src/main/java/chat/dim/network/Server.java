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
import chat.dim.fsm.StateDelegate;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Messenger;
import chat.dim.mtp.protocol.Package;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Command;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.sg.Star;
import chat.dim.sg.StarDelegate;
import chat.dim.sg.StarStatus;
import chat.dim.stargate.Gate;
import chat.dim.stargate.Ship;
import chat.dim.threading.BackgroundThreads;
import chat.dim.utils.Log;

public class Server extends Station implements MessengerDelegate, StarDelegate<Package>, StateDelegate<ServerState> {

    public final String name;

    private User currentUser = null;
    public String session = null;

    private final StateMachine fsm;

    private Gate star = null;
    final private ReadWriteLock starLock = new ReentrantReadWriteLock();

    private Map<String, Object> startOptions = null;

    StationDelegate delegate = null;

    private boolean paused = false;

    Server(ID identifier, String host, int port, String title) {
        super(identifier, host, port);
        name = title;
        // connection state machine
        fsm = new StateMachine(this);
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
        return fsm.getCurrentState();
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

    //---- slowly command for meta/profile

    private ReliableMessage<ID, SymmetricKey> packCommand(Command cmd) {
        InstantMessage<ID, SymmetricKey> iMsg = new InstantMessage<>(cmd, currentUser.identifier, identifier);
        Messenger messenger = Messenger.getInstance();
        SecureMessage<ID, SymmetricKey> sMsg = messenger.encryptMessage(iMsg);
        if (sMsg == null) {
            throw new NullPointerException("failed to encrypt message: " + iMsg);
        }
        ReliableMessage<ID, SymmetricKey> rMsg = messenger.signMessage(sMsg);
        if (rMsg == null) {
            throw new NullPointerException("failed to sign message: " + sMsg);
        }
        return rMsg;
    }

    public void sendSlowlyCommand(final Command cmd) {
        BackgroundThreads.wait(new Runnable() {
            @Override
            public void run() {
                ReliableMessage<ID, SymmetricKey> rMsg = packCommand(cmd);
                // send out directly
                Messenger messenger = Messenger.getInstance();
                byte[] data = messenger.serializeMessage(rMsg);
                send(data, Ship.SLOWER);
            }
        });
    }

    //---- urgent command for connection

    private void sendUrgentCommand(final HandshakeCommand cmd) {
        BackgroundThreads.rush(new Runnable() {
            @Override
            public void run() {
                ReliableMessage<ID, SymmetricKey> rMsg = packCommand(cmd);
                // first handshake?
                if (cmd.state == HandshakeCommand.HandshakeState.START) {
                    // [Meta protocol]
                    rMsg.setMeta(currentUser.getMeta());
                }
                // send out directly
                Messenger messenger = Messenger.getInstance();
                byte[] data = messenger.serializeMessage(rMsg);
                send(data, Ship.URGENT);
            }
        });
    }

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
//        ServerState state = getCurrentState();
//        if (!StateMachine.handshakingState.equals(state.name)) {
//            // FIXME: sometimes the connection state will be reset
//            return;
//        }
        // check connection status == 'Connected'
        if (getStatus() != StarStatus.Connected) {
            // FIXME: sometimes the connection will be lost while handshaking
            return;
        }
        if (newSession != null) {
            session = newSession;
        }
        // create handshake command
        HandshakeCommand cmd = new HandshakeCommand(session);
        setLastReceivedMessageTime(cmd);
        sendUrgentCommand(cmd);
    }

    public void handshakeAccepted() {
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!state.name.equals(ServerState.HANDSHAKING)) {
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
        if (!state.name.equals(ServerState.HANDSHAKING)) {
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
                setStar(new Gate(this));
            }
            Log.info("launching with options: " + options);
            star.launch(options);

            // TODO: post notification "StationConnecting"
        } finally {
            writeLock.unlock();
        }

        // TODO: let the subclass to create StarGate
    }

    private void setStar(Gate newStar) {
        Lock writeLock = starLock.writeLock();
        writeLock.lock();
        try {
            if (star != null) {
                star.disconnect();
                star.terminate();
            }
            star = newStar;
        } finally {
            writeLock.unlock();
        }
    }

    void end() {
        setStar(null);

        fsm.stop();
    }

    void pause() {
        if (paused) {
            return;
        }

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

        paused = true;
    }

    void resume() {
        if (!paused) {
            return;
        }
        paused = false;

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

    public boolean isPaused() {
        return paused;
    }

    private void send(byte[] payload, int priority) {
        Ship ship = new Ship(priority, payload, this);
        Lock readLock = starLock.readLock();
        readLock.lock();
        try {
            if (star != null) {
                star.send(ship);
            }
        } finally {
            readLock.unlock();
        }
    }

    private void restart() {
        if (startOptions == null) {
            return;
        }
        setStar(new Gate(this));
        Log.info("restart with options: " + startOptions);
        star.launch(startOptions);

        // TODO: post notification "StationConnecting"
    }

    //-------- StarDelegate

    @Override
    public void onReceived(Star star, Package response) {
        Log.info("received " + response.getLength() + " bytes");
        delegate.onReceivePackage(response.body.getBytes(), this);
    }

    @Override
    public void onStatusChanged(Star star, StarStatus oldStatus, StarStatus newStatus) {
        Log.info("status changed: " + oldStatus + " -> " + newStatus);
        fsm.tick();
    }

    @Override
    public void onSent(Star star, Package request, Error error) {
        Log.info("sent " + request.getLength() + " bytes");
        CompletionHandler handler = null;

        byte[] requestData = request.body.getBytes();
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
            if (state == null || !state.name.equals(ServerState.RUNNING)) {
                break;
            }
            wrapper = waitingList.remove(0);
            send(wrapper);
        }
    }

    private void send(RequestWrapper wrapper) {
        send(wrapper.data, wrapper.priority);

        if (wrapper.handler != null) {
            String key = RequestWrapper.getKey(wrapper.data);
            sendingTable.put(key, wrapper);
        }
    }

    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        return sendPackage(Ship.NORMAL, data, handler);
    }

    public boolean sendPackage(int priority, byte[] data, CompletionHandler handler) {
        RequestWrapper wrapper = new RequestWrapper(priority, data, handler);

        ServerState state = getCurrentState();
        if (state == null || !state.name.equals(ServerState.RUNNING)) {
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
    public void enterState(ServerState state, Machine machine) {
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
                // send all packages waiting
                sendAllWaiting();
                break;
            }
            case ServerState.ERROR: {
                // reconnect
                restart();
                break;
            }
        }
    }

    @Override
    public void exitState(ServerState state, Machine machine) {
    }

    @Override
    public void pauseState(ServerState state, Machine machine) {

    }

    @Override
    public void resumeState(ServerState state, Machine machine) {

    }
}
