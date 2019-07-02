package chat.dim.client;

import org.bouncycastle.util.Pack;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.core.CompletionHandler;
import chat.dim.core.TransceiverDelegate;
import chat.dim.crypto.Digest;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.mkm.User;
import chat.dim.mkm.entity.ID;
import chat.dim.network.Station;
import chat.dim.protocol.command.HandshakeCommand;
import chat.dim.protocol.file.FileContent;
import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.stargate.marsgate.Mars;

public class Server extends Station implements TransceiverDelegate, StarDelegate {

    public User currentUser;

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

    public void handshake(String session) {
        ID userID = currentUser == null ? null : currentUser.identifier;
        if (userID == null || !userID.isValid()) {
            throw new NullPointerException("current user error: " + currentUser);
        }
        // TODO: check FSM state == 'Handshaking'

        if (star == null || star.getStatus() != StarStatus.Connected) {
            // FIXME: sometimes the connection will be lost while handshaking
            return;
        }
        ID serverID = identifier;
        HandshakeCommand cmd = new HandshakeCommand(session);
        InstantMessage iMsg = new InstantMessage(cmd, userID, serverID);
        Transceiver transceiver = Transceiver.getInstance();
        ReliableMessage rMsg = null;
        try {
            rMsg = transceiver.encryptAndSignMessage(iMsg);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if (rMsg == null) {
            throw new NullPointerException("failed to encrypt and sign message: " + iMsg);
        }

        // first handshake?
        if (cmd.state == HandshakeCommand.START) {
            rMsg.setMeta(currentUser.getMeta());
        }

        // send out directly
        String json = JSON.encode(rMsg);
        byte[] data = json.getBytes(Charset.forName("UTF-8"));
        star.send(data);
    }

    public void handshakeAccepted(String session, boolean success) {
        // TODO: check FSM state == 'Handshaking'

        if (success) {
            //_fsm.session = session;
        }
    }

    public void start(Map<String, Object> options) {
        //_fsm.start()

        Transceiver transceiver = Transceiver.getInstance();
        transceiver.delegate = this;

        star = new Mars(this);
        star.launch(options);

        // TODO: perform run() in background
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
