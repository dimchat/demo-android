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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.CompletionHandler;
import chat.dim.ConnectionDelegate;
import chat.dim.MessengerDelegate;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.dkd.SecureMessage;
import chat.dim.format.JSON;
import chat.dim.fsm.Machine;
import chat.dim.fsm.State;
import chat.dim.fsm.StateDelegate;
import chat.dim.mkm.ID;
import chat.dim.mkm.User;
import chat.dim.model.Messenger;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.utils.Log;

public class Server extends Station implements MessengerDelegate, StarDelegate, StateDelegate {

    private User currentUser = null;
    public String session = null;

    final StateMachine fsm;

    public Star star = null;

    public StationDelegate delegate;
    public ConnectionDelegate messenger;

    public Server(ID identifier, String host, int port) {
        super(identifier, host, port);
        // connection state machine
        fsm = new StateMachine();
        fsm.server = this;
        fsm.delegate = this;
    }

    public Server(Map<String, Object> dictionary) {
        this(ID.getInstance(dictionary.get("ID")),
                (String) dictionary.get("host"),
                (int) dictionary.get("port"));

        // SP
        // CA
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
        if (newSession != null) {
            session = newSession;
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
        if (cmd.state == HandshakeCommand.START) {
            // [Meta protocol]
            rMsg.setMeta(currentUser.getMeta());
        }
        // send out directly
        String json = JSON.encode(rMsg);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        star.send(data);
    }

    public void handshakeAccepted(String newSession, boolean success) {
        // check FSM state == 'Handshaking'
        ServerState state = getCurrentState();
        if (!state.name.equals(StateMachine.handshakingState)) {
            // FIXME: sometimes the connection state will be reset
            return;
        }
        if (success) {
            Log.info("handshake accepted for user: " + currentUser);
            session = newSession;
            // TODO: broadcast profile to DIM network
        } else {
            // new session key from station
            Log.info("handshake again with session: " + newSession);
        }
    }

    //--------

    public void start(Map<String, Object> options) {

        Messenger messenger = Messenger.getInstance();
        messenger.setDelegate(this);

        fsm.start();
        star.launch(options);

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

    //-------- StarDelegate

    @Override
    public void onReceive(byte[] responseData, Star star) {
        byte[] response;
        try {
            response = messenger.receivedPackage(responseData);
        } catch (NullPointerException e) {
            e.printStackTrace();
            response = null;
        }
        if (response != null && response.length > 0) {
            star.send(response);
        }
    }

    @Override
    public void onStatusChanged(StarStatus status, Star star) {
        Log.info("status changed: " + status);
        fsm.tick();
    }

    @Override
    public void onFinishSend(byte[] requestData, Error error, Star star) {
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

        star.send(data);

        if (handler != null) {
            String key = RequestWrapper.getKey(data);
            sendingTable.put(key, wrapper);
        }

        return true;
    }

    @Override
    public String uploadFileData(byte[] data, InstantMessage iMsg) {
        ID sender = ID.getInstance(iMsg.envelope.sender);
        FileContent content = (FileContent) iMsg.content;
        String filename = content.getFilename();

        // TODO: upload onto FTP server
        return null;
    }

    @Override
    public byte[] downloadFileData(String url, InstantMessage iMsg) {
        // TODO: download from FTP server

        return new byte[0];
    }

    //-------- StateDelegate

    @Override
    public void enterState(State state, Machine machine) {
        ServerState serverState = (ServerState) state;
        if (serverState.name.equals(StateMachine.handshakingState)) {
            // start handshake
            handshake(null);
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
