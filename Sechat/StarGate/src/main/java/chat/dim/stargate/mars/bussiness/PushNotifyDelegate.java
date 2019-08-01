package chat.dim.stargate.mars.bussiness;

public interface PushNotifyDelegate {

    void notifyPushMessage(byte[] pushData, int cmdId);
}
