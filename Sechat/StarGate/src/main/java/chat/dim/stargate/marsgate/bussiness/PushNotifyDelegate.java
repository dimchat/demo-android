package chat.dim.stargate.marsgate.bussiness;

public interface PushNotifyDelegate {

    void notifyPushMessage(byte[] pushData, int cmdId);
}
