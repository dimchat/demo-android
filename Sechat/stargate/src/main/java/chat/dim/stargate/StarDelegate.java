package chat.dim.stargate;

public interface StarDelegate {

    int onReceive(byte[] responseData, Star star);

    void onConnectionStatusChanged(StarStatus status, Star star);

    void onFinishSend(byte[] requestData, Error error, Star star);
}
