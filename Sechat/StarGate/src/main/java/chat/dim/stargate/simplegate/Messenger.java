package chat.dim.stargate.simplegate;

import chat.dim.stargate.Star;
import chat.dim.stargate.StarDelegate;

public class Messenger {

    private byte[] requestData;
    private StarDelegate delegate;

    public Star star;

    public Messenger(byte[] data) {
        this(data, null);
    }

    public Messenger(byte[] data, StarDelegate sender) {
        super();
        requestData = data;
        delegate = sender;
    }

    public byte[] getRequestData() {
        return requestData;
    }

    public StarDelegate getDelegate() {
        return delegate;
    }

    public void onResponse(byte[] responseData) {
        delegate.onReceive(responseData, star);
    }

    public void onSuccess() {
        delegate.onFinishSend(requestData, null, star);
    }

    public void onError(Error error) {
        delegate.onFinishSend(requestData, error, star);
    }
}
