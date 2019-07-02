package chat.dim.stargate.marsgate.bussiness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkEvent implements NetworkDelegate {

    public static final String kConnectionStatusChanged = "ConnectionStatusChanged";

    public static final int SAY_HELLO = 1;
    public static final int CONVERSATION_LIST = 2;
    public static final int SEND_MSG = 3;
    public static final int PUSH_MSG = 10001;

    private Map<String, CGITask> tasks = new HashMap<>();
    private Map<String, NotifyDelegate> controllers = new HashMap<>();
    private Map<Integer, PushNotifyDelegate> pushReceivers = new HashMap<>();

    private Map<String, List<String>> ipTable = new HashMap<>();

    public NetworkEvent() {
        super();
    }

    public void setIPList(List<String> list, String host) {
        ipTable.put(host, list);
    }

    public void addIPAddress(String ip, String host) {
        List<String> list = ipTable.get(host);
        if (list == null) {
            list = new ArrayList<>();
            ipTable.put(host, list);
        }
        list.add(ip);
    }

    //-------- NetworkDelegate

    @Override
    public void addPushObserver(PushNotifyDelegate observer, int cmdId) {
        pushReceivers.put(cmdId, observer);
    }

    @Override
    public void addObserver(NotifyDelegate observer, String key) {
        controllers.put(key, observer);
    }

    @Override
    public void addCGITask(CGITask cgiTask, String key) {
        tasks.put(key, cgiTask);
    }

    @Override
    public boolean isAuthorized() {
        // TODO: check whether connection authorized
        return true;
    }

    @Override
    public List<String> onNewDns(String address) {
        List<String> list = ipTable.get(address);
        if (list == null && !address.equals("dim.chat")) {
            list = ipTable.get("dim.chat");
        }
        return list;
    }

    @Override
    public void onPush(int cmdId, byte[] data) {
        PushNotifyDelegate delegate = pushReceivers.get(cmdId);
        if (delegate != null) {
            delegate.notifyPushMessage(data, cmdId);
        }
    }

    @Override
    public byte[] request2Buffer(int taskId, CGITask cgiTask) {
        NotifyDelegate delegate = controllers.get(taskId);
        return delegate == null ? null : delegate.requestSendData();
    }

    @Override
    public int buffer2Response(int taskId, byte[] responseData, CGITask cgiTask) {
        NotifyDelegate delegate = controllers.get(taskId);
        return delegate == null ? -1 : delegate.onPostDecode(responseData);
    }

    @Override
    public int onTaskEnd(int taskId, CGITask cgiTask, int errType, int errCode) {
        tasks.remove(taskId);
        NotifyDelegate delegate = controllers.get(taskId);
        delegate.onTaskEnd(taskId, errType, errCode);
        controllers.remove(taskId);
        return 0;
    }

    @Override
    public void onConnectionStatusChange(int status, int longConnStatus) {
        // TODO: post notification
    }
}
