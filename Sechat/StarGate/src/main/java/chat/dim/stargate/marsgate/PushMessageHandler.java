package chat.dim.stargate.marsgate;

import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;

import chat.dim.stargate.mars.bussiness.PushNotifyDelegate;

public class PushMessageHandler implements PushNotifyDelegate {

    public Star star;

    private StarDelegate handler;

    public PushMessageHandler(StarDelegate receiver) {
        super();
        handler = receiver;
    }

    @Override
    public void notifyPushMessage(byte[] pushData, int cmdId) {
        // TODO: receive push message
        int res = handler.onReceive(pushData, star);
    }
}
