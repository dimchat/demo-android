package chat.dim.stargate.mars.bussiness;

public interface NotifyDelegate {

    byte[] requestSendData();

    int onPostDecode(byte[] responseData);

    int onTaskEnd(int tid, int errType, int errCode);
}
