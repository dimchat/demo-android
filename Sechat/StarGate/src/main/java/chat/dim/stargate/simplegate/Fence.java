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

        SocketClient(String host, int port) {
            super();
            this.host = host;
            this.port = port;
        }

        private int connect() {
            return connect(host, port);
        }
        private int connect(String host, int port) {
            try {
                socket = new Socket(host, port);
                connectionStatus = StarStatus.Connected;
            } catch (IOException e) {
                e.printStackTrace();
                connectionStatus = StarStatus.Error;
                return -1;
            }
            return 0;
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
                    write(task.getRequestData());
                    // response
                    read(task);
                    continue;
                }
                // sleep
                sleep(500);
            }
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

        Thread thread = new Thread(new SocketClient(host, port));
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
