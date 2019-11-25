package chat.dim.sechat.search.ui.search;

import android.arch.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.protocol.SearchCommand;

public class SearchViewModel extends ViewModel {

    SearchViewModel() {
        super();
        ID founder = facebook.getID("founder");
        assert founder != null;
        list.add(founder);
    }

    private final List<ID> list = new ArrayList<>();

    private Facebook facebook = Facebook.getInstance();

    public void updateSearchResult(SearchCommand cmd) {
        list.clear();
        List users = cmd.getUsers();
        if (users == null) {
            return;
        }
        ID identifier;
        for (Object item: users) {
            identifier = facebook.getID(item);
            if (identifier == null || !identifier.getType().isUser()) {
                continue;
            }
            list.add(identifier);
        }
    }

    public List<ID> getUsers() {
        return list;
    }
}
