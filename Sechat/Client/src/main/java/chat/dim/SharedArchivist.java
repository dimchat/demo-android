package chat.dim;

import java.util.Date;
import java.util.List;

import chat.dim.dbi.AccountDBI;
import chat.dim.protocol.ID;

class SharedArchivist extends ClientArchivist {

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

    @Override
    protected Date getLastGroupHistoryTime(ID group, List<ID> members) {
        CommonFacebook facebook = getFacebook();
        AccountDBI db = facebook.getDatabase();
        // TODO: get group history commands from local storage
        return null;
    }

}
