package chat.dim.stargate.mars.bussiness;

import java.util.List;

public interface NetworkDelegate {

    void addPushObserver(PushNotifyDelegate observer, int cmdId);
    void addObserver(NotifyDelegate observer, String key);
    void addCGITask(CGITask cgiTask, String key);

    boolean isAuthorized();
    List<String> onNewDns(String address);
    void onPush(int cmdId, byte[] data);

    byte[] request2Buffer(int taskId, CGITask cgiTask);
    int buffer2Response(int taskId, byte[] responseData, CGITask cgiTask);

    int onTaskEnd(int taskId, CGITask cgiTask, int errType, int errCode);
    void onConnectionStatusChange(int status, int longConnStatus);
}
