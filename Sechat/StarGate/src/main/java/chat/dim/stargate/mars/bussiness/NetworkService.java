package chat.dim.stargate.mars.bussiness;

import com.tencent.mars.BaseEvent;
import com.tencent.mars.Mars;
import com.tencent.mars.stn.StnLogic;

import java.util.ArrayList;
import java.util.List;

public class NetworkService implements NetworkStatusDelegate {
    private static final NetworkService ourInstance = new NetworkService();

    public static NetworkService getInstance() {
        return ourInstance;
    }

    private NetworkService() {
    }

    public NetworkDelegate delegate;

    public void setCallback() {
        // TODO:
//        mars::stn::SetCallback(mars::stn::StnCallBack::Instance());
//        mars::app::SetCallback(mars::app::AppCallBack::Instance());
    }

    public void createMars() {
        Mars.onCreate(true);
    }

    public void setClientVersion(int clientVersion) {
        StnLogic.setClientVersion(clientVersion);
    }

    public void setShortLinkDebug(String debugIP, short port) {
        StnLogic.setShortlinkSvrAddr(port, debugIP);
    }

    public void setShortLinkPort(short port) {
        StnLogic.setShortlinkSvrAddr(port);
    }

    public void setLongLingAddress(String address, short port, String debugIP) {
        StnLogic.setLonglinkSvrAddr(address, new int[]{port}, debugIP);
    }

    public void setLongLingAddress(String address, short port) {
        StnLogic.setLonglinkSvrAddr(address, new int[]{port});
    }

    public void makesureLongLinkConnect() {
        StnLogic.makesureLongLinkConnected();
    }

    public void destroyMars() {
        Mars.onDestroy();
    }

    public void addPushObserver(PushNotifyDelegate observer, int cmdId) {
        delegate.addPushObserver(observer, cmdId);
    }

    public int startTask(CGITask cgiTask, NotifyDelegate delegateUI) {
        ArrayList<String> shortLinkHostList = new ArrayList<>();
        shortLinkHostList.add(cgiTask.host);

        StnLogic.Task _task = new StnLogic.Task(StnLogic.Task.EShort, cgiTask.cmdId, cgiTask.cgi, shortLinkHostList);
        _task.channelSelect = cgiTask.channelSelect;
        _task.userContext = cgiTask;

        String taskIdKey = String.valueOf(_task.taskID);
        delegate.addObserver(delegateUI, taskIdKey);
        delegate.addCGITask(cgiTask, taskIdKey);

        StnLogic.startTask(_task);
        return _task.taskID;
    }

    public void stopTask(int taskId) {
        StnLogic.stopTask(taskId);
    }

    // event reporting
    public void reportEvent_OnForeground(boolean isForeground) {
        BaseEvent.onForeground(isForeground);
    }

    public void reportEvent_OnNetworkChange() {
        BaseEvent.onNetworkChange();
    }

    // callbacks

    public boolean isAuthorized() {
        return delegate.isAuthorized();
    }

    public List<String> onNewDNS(String address) {
        return delegate.onNewDns(address);
    }

    public void onPush(int cmdId, byte[] data) {
        delegate.onPush(cmdId, data);
    }

    public byte[] request2Buffer(int taskId, Object userContext) {
        CGITask cgiTask = (CGITask) userContext;
        return delegate.request2Buffer(taskId, cgiTask);
    }

    public int buffer2Response(int taskId, byte[] responseData, Object userContext) {
        CGITask cgiTask = (CGITask) userContext;
        return delegate.buffer2Response(taskId, responseData, cgiTask);
    }

    public int onTaskEnd(int taskId, Object userContext, int errType, int errCode) {
        CGITask cgiTask = (CGITask) userContext;
        return delegate.onTaskEnd(taskId, cgiTask, errType, errCode);
    }

    public void onConnectionStatusChange(int status, int longConnStatus) {
        delegate.onConnectionStatusChange(status, longConnStatus);
    }

    //-------- NetworkStatusDelegate

    @Override
    public void reachabilityChange(int uiFlags) {
        // TODO: check uiFlags
        BaseEvent.onNetworkChange();
    }
}
