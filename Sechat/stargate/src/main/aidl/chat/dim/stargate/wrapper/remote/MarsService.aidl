// IMarsService.aidl
package chat.dim.stargate.wrapper.remote;

// Declare any non-default types here with import statements

import chat.dim.stargate.wrapper.remote.MarsTaskWrapper;
import chat.dim.stargate.wrapper.remote.MarsPushMessageFilter;

interface MarsService {

    int send(MarsTaskWrapper taskWrapper, in Bundle taskProperties);

    void cancel(int taskID);

    void registerPushMessageFilter(MarsPushMessageFilter filter);

    void unregisterPushMessageFilter(MarsPushMessageFilter filter);

    void setAccountInfo(in long uin, in String userName);

    void setForeground(in int isForeground);
}
