package chat.dim;

import chat.dim.dbi.AccountDBI;
import chat.dim.dbi.MessageDBI;
import chat.dim.dbi.SessionDBI;

public enum GlobalVariable {

    INSTANCE;

    public static GlobalVariable getInstance() {
        return INSTANCE;
    }

    GlobalVariable() {
        SharedDatabase db = new SharedDatabase();
        adb = db;
        mdb = db;
        sdb = db;
        database = db;
        archivist = new SharedArchivist(db);
        facebook = new SharedFacebook();
        emitter = new Emitter();

        CryptoPlugins.registerCryptoPlugins();

        Register.prepare();
    }

    public final AccountDBI adb;
    public final MessageDBI mdb;
    public final SessionDBI sdb;
    public final SharedDatabase database;

    public final ClientArchivist archivist;
    public final SharedFacebook facebook;

    public final Emitter emitter;

    public SharedMessenger messenger = null;
    public Terminal terminal = null;
}
