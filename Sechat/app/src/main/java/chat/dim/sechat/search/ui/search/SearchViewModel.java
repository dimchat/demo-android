package chat.dim.sechat.search.ui.search;

import android.arch.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.protocol.SearchCommand;
import chat.dim.utils.Log;

public class SearchViewModel extends ViewModel {

    private Facebook facebook = Facebook.getInstance();

    private SearchCommand result = null;

    public void updateSearchResult(SearchCommand cmd) {
        result = cmd;
        Log.info("search result: " + getUsers().size());
    }

    public List<ID> getUsers() {
        List<ID> mArray = new ArrayList<>();
        if (result != null) {
            List users = result.getUsers();
            if (users != null) {
                ID identifier;
                for (Object item: users) {
                    identifier = facebook.getID(item);
                    assert identifier != null;
                    mArray.add(identifier);
                }
            }
        }
        if (mArray.size() == 0) {
            mArray.add(facebook.getID("founder"));
        }
        return mArray;
    }
}
