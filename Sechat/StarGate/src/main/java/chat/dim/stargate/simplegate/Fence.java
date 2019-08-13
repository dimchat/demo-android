package chat.dim.stargate.simplegate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;

public class Fence implements Star {

    private StarStatus connectionStatus = StarStatus.Init;

    private StarDelegate handler;

    private List<Messenger> waitingList = new ArrayList<>();

    public Fence(StarDelegate messageHandler) {
        super();

        handler = messageHandler;
    }

    class SocketClient implements Runnable {

        private String host;
        private int port;
        private Socket socket = null;

        Star star = null;

        SocketClient(String host, int port) {
            super();
            this.host = host;
            this.port = port;
        }

        private int connect() {
            return connect(host, port);
        }
        private int connect(String host, int port) {
            // switch status to 'Connecting'
            connectionStatus = StarStatus.Connecting;
            handler.onConnectionStatusChanged(connectionStatus, star);
            try {
                socket = new Socket(host, port);
                // switch status to 'Connected'
                connectionStatus = StarStatus.Connected;
            } catch (IOException e) {
                e.printStackTrace();
                // switch status to 'Error'
                connectionStatus = StarStatus.Error;
            }
            handler.onConnectionStatusChanged(connectionStatus, star);
            return connectionStatus == StarStatus.Error ? -1 : 0;
        }

        private int read(Messenger messenger) {
            InputStream inputStream;
            int length = 0;
            byte[] responseData = null;
            try {
                inputStream = socket.getInputStream();
                length = inputStream.available();
                if (length <= 0) {
                    return length;
                }
                responseData = new byte[length];
                int count = inputStream.read(responseData);
                assert count == length;
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
            if (messenger == null) {
                onReceive(responseData);
            } else {
                messenger.onResponse(responseData);
                messenger.onSuccess();
            }
            return length;
        }

        private void write(byte[] data) {
            try {
                OutputStream outputStream = socket.getOutputStream();
                outputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sleep(long millis) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private byte[] packRequestData(Messenger task) {
            // TODO: the python station support two packing format now
            //    1. NetMsg format
            //    2. Plaintext format ends with '\n'
            byte[] data = task.getRequestData();
            byte[] pack = new byte[data.length + 1];
            System.arraycopy(data, 0, pack, 0, data.length);
            pack[data.length] = '\n';
            return pack;
        }

        @Override
        public void run() {
            connect();
            if (socket == null) {
                throw new NullPointerException("socket not connected");
            }
            while (socket.isConnected()) {
                if (read(null) > 0) {
                    // push message
                    continue;
                }
                if (waitingList.size() > 0) {
                    // send
                    Messenger task = waitingList.remove(0);
                    write(packRequestData(task));
                    // response
                    read(task);
                    continue;
                }
                // sleep
                sleep(500);
            }
            connectionStatus = StarStatus.Error;
            handler.onConnectionStatusChanged(connectionStatus, star);
        }
    }

    public void onReceive(byte[] pushData) {
        handler.onReceive(pushData, this);
    }

    //-------- Star

    @Override
    public StarStatus getStatus() {
        return connectionStatus;
    }

    @Override
    public boolean launch(Map<String, Object> options) {

        String host = (String) options.get("host");
        int port = (int) options.get("port");

        SocketClient sock = new SocketClient(host, port);
        sock.star = this;

        Thread thread = new Thread(sock);
        thread.start();

        return true;
    }

    @Override
    public void terminate() {

    }

    @Override
    public void enterBackground() {

    }

    @Override
    public void enterForeground() {

    }

    @Override
    public int send(byte[] requestData) {
        return send(requestData, handler);
    }

    @Override
    public int send(byte[] requestData, StarDelegate messageHandler) {

        Messenger task = new Messenger(requestData, messageHandler);
        task.star = this;
        waitingList.add(task);

        return 0;
    }
}
