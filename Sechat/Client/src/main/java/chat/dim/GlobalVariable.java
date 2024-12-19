package chat.dim;

import chat.dim.dbi.AccountDBI;
import chat.dim.group.SharedGroupManager;

public enum GlobalVariable {

    INSTANCE;

    public static GlobalVariable getInstance() {
        return INSTANCE;
    }

    GlobalVariable() {
        database = new SharedDatabase();
        facebook = createFacebook(database);
        emitter = new Emitter();

        CryptoPlugins.registerCryptoPlugins();

        Register.prepare();
    }

    public final SharedDatabase database;
    public final SharedFacebook facebook;
    public final Emitter emitter;

    public SharedMessenger messenger = null;
    public Terminal terminal = null;

    public void setMessenger(SharedMessenger transceiver) {
        messenger = transceiver;
        // prepare for group manager
        SharedGroupManager manager = SharedGroupManager.getInstance();
        manager.setFacebook(facebook);
        manager.setMessenger(messenger);
        // prepare for entity checker
        ClientChecker checker = facebook.getEntityChecker();
        checker.setMessenger(messenger);
    }

    static SharedFacebook createFacebook(AccountDBI db) {
        SharedFacebook facebook = new SharedFacebook(db);
        facebook.setArchivist(new ClientArchivist(facebook, db));
        facebook.setEntityChecker(new ClientChecker(facebook, db));
        return facebook;
    }

}
