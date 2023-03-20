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
        facebook = new SharedFacebook(db);

        CryptoPlugins.registerCryptoPlugins();

        Register.prepare();
    }

    public AccountDBI adb;
    public MessageDBI mdb;
    public SessionDBI sdb;
    public SharedDatabase database;

    public SharedFacebook facebook;

    public SharedMessenger messenger = null;
    public Terminal terminal = null;

    public Emitter emitter = new Emitter().start();
}
