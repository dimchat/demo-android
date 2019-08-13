package chat.dim.stargate.marsgate;

import android.app.Application;

import com.tencent.mars.stn.StnLogic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.StarStatus;
import chat.dim.stargate.mars.bussiness.CGITask;
import chat.dim.stargate.mars.bussiness.ChannelType;
import chat.dim.stargate.mars.bussiness.NetworkEvent;
import chat.dim.stargate.mars.bussiness.NetworkService;

import static chat.dim.stargate.mars.bussiness.NetworkEvent.SEND_MSG;

public class Mars implements Star {

    private StarStatus longConnectionStatus = StarStatus.Init;
    private StarStatus connectionStatus = StarStatus.Init;

    private StarDelegate handler;
    private PushMessageHandler pushHandler;

    public Mars(StarDelegate messageHandler) {
        super();

        handler = messageHandler;

        pushHandler = new PushMessageHandler(handler);
        pushHandler.star = this;
    }

    private void onConnectionStatusChanged(Map<String, Object> notification) {

        int longlink_status = StnLogic.NETWORK_UNKNOWN;
        int conn_status = StnLogic.NETWORK_UNKNOWN;

        Object status;
        status = notification.get("LongConnectionStatus");
        if (status != null) {
            longlink_status = (int) status;
        }
        status = notification.get("ConnectionStatus");
        if (status != null) {
            conn_status = (int) status;
        }

        switch (longlink_status) {
            case StnLogic.NETWORK_UNAVAILABLE:
            case StnLogic.SERVER_FAILED:
            case StnLogic.SERVER_DOWN:
            case StnLogic.GATEWAY_FAILED: {
                longConnectionStatus = StarStatus.Error;
                break;
            }

            case StnLogic.CONNECTTING: {
                longConnectionStatus = StarStatus.Connecting;
                break;
            }

            case StnLogic.CONNECTED: {
                longConnectionStatus = StarStatus.Connected;
                break;
            }

            case StnLogic.NETWORK_UNKNOWN: {
                longConnectionStatus = StarStatus.Error;
                break;
            }
        }

        switch (conn_status) {
            case StnLogic.NETWORK_UNAVAILABLE:
            case StnLogic.SERVER_FAILED:
            case StnLogic.SERVER_DOWN:
            case StnLogic.GATEWAY_FAILED: {
                connectionStatus = StarStatus.Error;
                break;
            }

            case StnLogic.CONNECTTING: {
                connectionStatus = StarStatus.Connecting;
                break;
            }

            case StnLogic.CONNECTED: {
                connectionStatus = StarStatus.Connected;
                break;
            }

            case StnLogic.NETWORK_UNKNOWN: {
                connectionStatus = StarStatus.Error;
                break;
            }
        }

        handler.onConnectionStatusChanged(getStatus(), this);
    }

    //-------- Star

    @Override
    public StarStatus getStatus() {
        switch (longConnectionStatus) {
            case Init: {
                break;
            }

            case Connecting: {
                return longConnectionStatus;
            }

            case Connected: {
                return longConnectionStatus;
            }

            case Error: {
                break;
            }
        }
        return connectionStatus;
    }

//    @SuppressWarnings("unchecked")
    @Override
    public boolean launch(Map<String, Object> options) {

        Application app = (Application) options.get("Application");

        int clientVersion = 200;
        String longLinkAddress = "dim.chat";
        short longLinkPort = 9394;
        short shortLinkPort = 8080;

        Object value;
        // LongLink
        value = options.get("LongLinkAddress");
        if (value != null) {
            longLinkAddress = (String) value;
        }
        value = options.get("LongLinkPort");
        if (value != null) {
            longLinkPort = (short) value;
        }
        // ShortLink
        value = options.get("ShortLinkPort");
        if (value != null) {
            shortLinkPort = (short) value;
        }

        // OnNewDNS
        Map<String, List<String>> ipTable;
        value = options.get("NewDNS");
        if (value != null) {
            ipTable = (Map<String, List<String>>) value;
        } else {
            List<String> ipList = new ArrayList<>();
            ipList.add("127.0.0.1");
            ipTable = new HashMap<>();
            ipTable.put("dim.chat", ipList);
        }

        NetworkEvent networkEvent = new NetworkEvent();
        for (Map.Entry<String, List<String>> entry : ipTable.entrySet()) {
            networkEvent.setIPList(entry.getValue(), entry.getKey());
        }

//        NetworkService networkService = NetworkService.getInstance();
//        networkService.setCallback();
//        networkService.createMars();
//        networkService.setClientVersion(clientVersion);
//        networkService.setLongLingAddress(longLinkAddress, longLinkPort);
//        networkService.setShortLinkPort(shortLinkPort);
//        networkService.reportEvent_OnForeground(true);
//        networkService.makesureLongLinkConnect();
//
//        NetworkStatus networkStatus = NetworkStatus.getInstance();
//        networkStatus.start(networkService);
//
//        networkService.addPushObserver(pushHandler, SEND_MSG);
//        networkService.addPushObserver(pushHandler, PUSH_MSG);

        // TODO: listening ConnectionStatusChanged from networkEvent

        return true;
    }

    @Override
    public void terminate() {
        // TODO: remove listening ConnectionStatusChanged from networkEvent

        NetworkService.getInstance().destroyMars();
    }

    @Override
    public void enterBackground() {
        NetworkService.getInstance().reportEvent_OnForeground(false);
    }

    @Override
    public void enterForeground() {
        NetworkService.getInstance().reportEvent_OnForeground(true);
    }

    @Override
    public int send(byte[] requestData) {
        return send(requestData, handler);
    }

    @Override
    public int send(byte[] requestData, StarDelegate messageHandler) {

        Messenger messenger = new Messenger(requestData, messageHandler);
        messenger.star = this;

        CGITask cgiTask = new CGITask(ChannelType.LongConn,
                SEND_MSG, "/sendmessage", "dim.chat");
        NetworkService.getInstance().startTask(cgiTask, messenger);

        return 0;
    }
}
