package chat.dim;

import chat.dim.dbi.AccountDBI;

class SharedArchivist extends ClientArchivist {

    public SharedArchivist(AccountDBI db) {
        super(db);
    }

    @Override
    protected CommonFacebook getFacebook() {
        GlobalVariable shared = GlobalVariable.getInstance();
        return shared.facebook;
    }

    @Override
    protected CommonMessenger getMessenger() {
        GlobalVariable shared = GlobalVariable.getInstance();
        return shared.messenger;
    }

}
