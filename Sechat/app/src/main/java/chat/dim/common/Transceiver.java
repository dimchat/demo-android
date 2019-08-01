package chat.dim.common;

public class Transceiver extends chat.dim.core.Transceiver {

    private static final Transceiver ourInstance = new Transceiver();

    public static Transceiver getInstance() {
        return ourInstance;
    }

    private Transceiver()  {
        super();

        barrack = Facebook.getInstance();
        keyCache = KeyStore.getInstance();
    }
}
