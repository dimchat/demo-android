package chat.dim.stargate.marsgate.bussiness;

interface NetworkStatusDelegate {

    void reachabilityChange(int uiFlags);
}

public class NetworkStatus {
    private static final NetworkStatus ourInstance = new NetworkStatus();

    public static NetworkStatus getInstance() {
        return ourInstance;
    }

    private NetworkStatus() {
    }

    NetworkStatusDelegate networkStatusDelegate;

    public void start(NetworkStatusDelegate delegate) {
        networkStatusDelegate = delegate;

        // TODO: reach ability
    }

    public void stop() {
        // TODO: reach ability

        networkStatusDelegate = null;
    }

    public void changeReach() {
        int connFlags = 0;

        // TODO: reach ability

        if (networkStatusDelegate != null) {
            networkStatusDelegate.reachabilityChange(connFlags);
        }
    }
}
