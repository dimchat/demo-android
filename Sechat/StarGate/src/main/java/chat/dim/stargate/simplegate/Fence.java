package chat.dim.stargate.simplegate;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
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

        private byte[] process(byte[] data, Messenger task) {
            if (data == null || data.length < 3) {
                return null;
            }
            if (data[0] != '{') {
                throw new IllegalArgumentException("unknown protocol: " + data);
            }
            int pos = 0;
            while (++pos < data.length) {
                if (data[pos] == '\n') {
                    break;
                }
            }
            if (pos >= data.length) {
                // incomplete data
                return data;
            }
            pos += 1;
            byte[] pack;
            byte[] tail;
            if (pos == data.length) {
                pack = data;
                tail = null;
            } else {
                pack = new byte[pos];
                System.arraycopy(data, 0, pack, 0, pos);
                // next package
                tail = new byte[data.length - pos];
                System.arraycopy(data, pos, tail, 0, data.length - pos);
            }

            if (task == null) {
                onReceive(pack);
            } else {
                task.onResponse(pack);
                task.onSuccess();
            }

            return tail;
        }

        private byte[] read(byte[] incomplete) {
            byte[] data;
            int length;
            try {
                InputStream inputStream = socket.getInputStream();
                length = inputStream.available();
                if (length <= 0) {
                    return incomplete;
                }
                data = new byte[length];
                int count = inputStream.read(data);
                assert count == length;
            } catch (IOException e) {
                e.printStackTrace();;
                return incomplete;
            }
            if (incomplete == null) {
                return data;
            }
            // merge with incomplete data
            int incompleteLength = incomplete.length;
            byte[] total = new byte[incompleteLength + length];
            System.arraycopy(incomplete, 0, total, 0, incompleteLength);
            System.arraycopy(data, 0, total, incompleteLength, length);
            return total;
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
            byte[] data = null;
            while (socket.isConnected()) {
                data = read(data);
                if (data != null) {
                    // push message
                    data = process(data, null);
                    continue;
                }
                if (waitingList.size() > 0) {
                    // send
                    Messenger task = waitingList.remove(0);
                    write(packRequestData(task));
                    // response
                    data = read(data);
                    data = process(data, task);
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
