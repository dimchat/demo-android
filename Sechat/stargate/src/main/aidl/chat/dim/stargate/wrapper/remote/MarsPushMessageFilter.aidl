// MarsRecvCallBack.aidl
package chat.dim.stargate.wrapper.remote;

// Declare any non-default types here with import statements

interface MarsPushMessageFilter {

    // returns processed ?
    boolean onRecv(int cmdId, inout byte[] buffer);

}
