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

import chat.dim.client.Messanger;
import chat.dim.core.CompletionHandler;
import chat.dim.core.TransceiverDelegate;
import chat.dim.crypto.Digest;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.fsm.Machine;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.mkm.entity.Meta;
import chat.dim.protocol.CommandContent;
import chat.dim.protocol.command.HandshakeCommand;
import chat.dim.protocol.file.FileContent;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.stargate.simplegate.Fence;

public class Server extends Station implements TransceiverDelegate, StarDelegate {

    public User currentUser;

    private Machine fsm;
    private Star star;

    public Server(ID identifier) {
        super(identifier);
    }

    public Server(ID identifier, String host, int port) {
        super(identifier, host, port);
    }

    public Server(Map<String, Object> dictionary) {
        this(ID.getInstance(dictionary.get("ID")),
                (String) dictionary.get("host"),
                (int) dictionary.get("port"));

        // SP
        // CA
    }

    public StarStatus getStatus() {
        return star.getStatus();
    }

    public void sendCommand(CommandContent cmd) {
        sendCommand(cmd, null);
    }

    private void sendCommand(CommandContent cmd, Meta meta) {
        InstantMessage iMsg = new InstantMessage(cmd, currentUser.identifier, identifier);
        if (cmd.command.equals(CommandContent.HANDSHAKE)) {
            HandshakeCommand handshake = (HandshakeCommand)cmd;
            // first handshake?
            if (handshake.state == HandshakeCommand.START) {
                iMsg.put("meta", currentUser.getMeta());
                //iMsg.put("profile", currentUser.getProfile());
            }
        }
        Messanger messanger = Messanger.getInstance();
        ReliableMessage rMsg = null;
        try {
            rMsg = messanger.encryptAndSignMessage(iMsg);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        if (rMsg == null) {
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }
        // send out directly
        String json = JSON.encode(rMsg) + "\n";
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        star.send(data);
    }

    public void handshake(String session) {
        // TODO: check FSM state == 'Handshaking'

        if (star == null || star.getStatus() != StarStatus.Connected) {
            // FIXME: sometimes the connection will be lost while handshaking
        }
        HandshakeCommand cmd = new HandshakeCommand(session);
        if (cmd.state == HandshakeCommand.START) {
            sendCommand(cmd, currentUser.getMeta());
        } else {
            sendCommand(cmd, null);
        }
    }

    public void handshakeAccepted(String session, boolean success) {
        // TODO: check FSM state == 'Handshaking'

        if (success) {
            //_fsm.session = session;
        }
    }

    public void start(Map<String, Object> options) {
        //_fsm.start()

        Messanger messanger = Messanger.getInstance();
        messanger.delegate = this;

        star = new Fence(this);
        //star = new Mars(this);
        star.launch(options);
    }

    public void end() {
        star.terminate();
        //_fsm.stop();
    }

    public void pause() {
        star.enterBackground();
        //_fsm.pause();
    }

    public void resume() {
        star.enterForeground();
        //_fsm.resume();
    }

    private void run() {
        // TODO: fsm.tick()
    }

    //---- TransceiverDelegate

    private List<PackageHandler> waitingList = new ArrayList<>();
    private Map<String, PackageHandler> sendingTable = new HashMap<>();

    class PackageHandler {
        byte[] data;
        CompletionHandler handler;

        PackageHandler(byte[] data, CompletionHandler handler) {
            super();
            this.data = data;
            this.handler = handler;
        }
    }
    private String getPackageHandlerKey(byte[] data) {
        byte[] hash = Digest.sha256(data);
        return Base64.encode(hash);
    }

    @Override
    public boolean sendPackage(byte[] data, CompletionHandler handler) {
        PackageHandler wrapper = new PackageHandler(data, handler);

        // TODO: check FSM.state == 'Running'

        int res = star.send(data);

        if (handler != null) {
            String key = getPackageHandlerKey(data);
            sendingTable.put(key, wrapper);
        }

        return res == 0;
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

    //-------- StarDelegate

    @Override
    public int onReceive(byte[] responseData, Star star) {
        delegate.didReceivePackage(responseData, this);
        return 0;
    }

    @Override
    public void onConnectionStatusChanged(StarStatus status, Star star) {
        //_fsm.tick()
    }

    @Override
    public void onFinishSend(byte[] requestData, Error error, Star star) {
        CompletionHandler handler = null;

        String key = getPackageHandlerKey(requestData);
        PackageHandler wrapper = sendingTable.get(key);
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
}
