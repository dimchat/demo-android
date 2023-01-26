package chat.dim;

import chat.dim.dbi.AccountDBI;
import chat.dim.dbi.MessageDBI;
import chat.dim.dbi.SessionDBI;
import chat.dim.http.HTTPClient;
import chat.dim.network.FtpServer;

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

        HTTPClient http = HTTPClient.getInstance();
        FtpServer ftp = FtpServer.getInstance();
        http.setDelegate(ftp);
        http.start();
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
