package chat.dim.common;

import chat.dim.database.KeyStore;

public class Transceiver extends chat.dim.core.Transceiver {

    private static final Transceiver ourInstance = new Transceiver();

    public static Transceiver getInstance() {
        return ourInstance;
    }

    private Transceiver()  {
        super();

        barrackDelegate = Facebook.getInstance();
        entityDataSource = Facebook.getInstance();
        cipherKeyDataSource = keyStore;
    }

    private final KeyStore keyStore = new KeyStore();
}
