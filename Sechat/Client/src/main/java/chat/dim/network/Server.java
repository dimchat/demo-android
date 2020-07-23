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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.CompletionHandler;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.MessengerDelegate;
import chat.dim.ReliableMessage;
import chat.dim.SecureMessage;
import chat.dim.User;
import chat.dim.filesys.ExternalStorage;
import chat.dim.fsm.Machine;
import chat.dim.fsm.State;
import chat.dim.fsm.StateDelegate;
import chat.dim.model.Messenger;
import chat.dim.notification.NotificationCenter;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.stargate.wormhole.Hole;
import chat.dim.utils.Log;

public class Server extends Station implements MessengerDelegate, StarDelegate, StateDelegate {

    public static final String ServerStateChanged = "ServerStateChanged";

    private User currentUser = null;
    public String session = null;

    final StateMachine fsm;

    private Star star = null;

    public StationDelegate delegate;

    public Server(ID identifier, String host, int port) {
        super(identifier, host, port);
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
        return star.getStatus();
    }

    //---- urgent command for connection

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
        // create handshake command
        HandshakeCommand cmd = new HandshakeCommand(session);
        InstantMessage iMsg = new InstantMessage(cmd, currentUser.identifier, identifier);
        Messenger messenger = Messenger.getInstance();
        SecureMessage sMsg = messenger.encryptMessage(iMsg);
        ReliableMessage rMsg = messenger.signMessage(sMsg);
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

    public void handshakeAccepted(String sessionKey, boolean success) {
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!state.name.equals(StateMachine.handshakingState)) {
            // FIXME: sometimes the connection state will be reset
        }
        if (success) {
            Log.info("handshake accepted for user: " + currentUser);
            session = sessionKey;
            // call client
            delegate.onHandshakeAccepted(sessionKey, this);
        } else {
            // new session key from station
            Log.info("handshake again with session: " + sessionKey);
        }
    }

//    public void connect(String host, int port) {
//        // fsm.changeState(fsm.defaultState);
//
//        if (getStatus().equals(StarStatus.Connected)
//                && getHost().equals(host) && getPort() == port) {
//            Log.info("already connected to " + host + ":" + port);
//            return;
//        }
//
//        // TODO: post notification "Connecting"
//
//        star.connect(host, port);
//
//        // setHost(host);
//        // setPort(port);
//    }

    //--------

    public void start(Map<String, Object> options) {

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

        if (star == null) {
            star = new Hole(this);
        }

        // TODO: post notification "StationConnecting"

        star.launch(options);

//        setHost(options.get("host"));
//        setPort(options.get("port"));

        // TODO: let the subclass to create StarGate
    }

    public void end() {
        star.terminate();
        fsm.stop();
    }

    public void pause() {
        star.enterBackground();
        fsm.pause();
    }

    public void resume() {
        star.enterForeground();
        fsm.resume();
    }

    public void send(byte[] payload) {
        star.send(payload);
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

    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        RequestWrapper wrapper = new RequestWrapper(data, handler);

        ServerState state = getCurrentState();
        if (!state.name.equals(StateMachine.runningState)) {
            waitingList.add(wrapper);
            return true;
        }

        send(data);

        if (handler != null) {
            String key = RequestWrapper.getKey(data);
            sendingTable.put(key, wrapper);
        }

        return true;
    }

    @Override
    public String uploadData(byte[] data, InstantMessage iMsg) {
        ID sender = ID.getInstance(iMsg.envelope.sender);
        FileContent content = (FileContent) iMsg.content;
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
        nc.postNotification(ServerStateChanged, this, info);

        if (serverState.name.equals(StateMachine.handshakingState)) {
            // start handshake
            String session = this.session;
            this.session = null;
            handshake(session);
        } else if (serverState.name.equals(StateMachine.runningState)) {
            // TODO: send all packages waiting
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
