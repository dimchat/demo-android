package chat.dim.common;

import chat.dim.database.KeyStore;

public class Transceiver extends chat.dim.core.Transceiver {

    private static final Transceiver ourInstance = new Transceiver();

    public static Transceiver getInstance() {
        return ourInstance;
    }

    private Transceiver()  {
        super();

        barrack = Facebook.getInstance();
        keyCache = new KeyStore();
    }
}
