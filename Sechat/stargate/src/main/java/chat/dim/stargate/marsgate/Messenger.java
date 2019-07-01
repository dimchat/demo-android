package chat.dim.stargate.marsgate;

import com.tencent.mars.stn.StnLogic;

import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;
import chat.dim.stargate.marsgate.bussiness.NotifyDelegate;

public class Messenger implements NotifyDelegate {

    private byte[] requestData;
    private StarDelegate starDelegate;

    public Star star;

    public Messenger(byte[] data) {
        this(data, null);
    }

    public Messenger(byte[] data, StarDelegate sender) {
        super();
        requestData = data;
        starDelegate = sender;
    }

    //-------- Messenger

    @Override
    public byte[] requestSendData() {
        return requestData;
    }

    @Override
    public int onPostDecode(byte[] responseData) {
        return starDelegate.onReceive(responseData, star);
    }

    @Override
    public int onTaskEnd(int tid, int errType, int errCode) {
        Error error = null;
        switch (errType) {
            case StnLogic.ectOK: {
                error = null;
                break;
            }
            case StnLogic.ectFalse: {
                error = new Error("OS Status Error");
                break;
            }
            case StnLogic.ectDial: {
                error = new Error("OS Status Error");
                break;
            }
            case StnLogic.ectDns: {
                error = new Error("URL Error");
                break;
            }
            case StnLogic.ectSocket: {
                error = new Error("Stream SOCKS Error");
                break;
            }
            case StnLogic.ectHttp: {
                error = new Error("URL Error");
                break;
            }
            case StnLogic.ectNetMsgXP: {
                error = new Error("Item Provider Error");
                break;
            }
            case StnLogic.ectEnDecode: {
                error = new Error("POSIX Error");
                break;
            }
            case StnLogic.ectServer: {
                error = new Error("Net Services Error");
                break;
            }
            case StnLogic.ectLocal: {
                error = new Error("Error");
                break;
            }
        }
        starDelegate.onFinishSend(requestData, error, star);
        return 0;
    }
}
