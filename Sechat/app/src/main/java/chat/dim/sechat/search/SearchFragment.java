package chat.dim.sechat.search;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import java.util.Map;

import chat.dim.ID;
import chat.dim.model.Messenger;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.SearchCommand;
import chat.dim.sechat.R;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.list.Listener;

public class SearchFragment extends ListFragment<RecyclerViewAdapter, DummyContent> implements Observer {

    private SearchView searchView;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SearchFragment() {
        super();

        dummyList = new DummyContent();
        Listener listener = (Listener<RecyclerViewAdapter.ViewHolder>) viewHolder -> {
            ID identifier = viewHolder.item.getIdentifier();
            assert getContext() != null : "fragment context error";
            Intent intent = new Intent();
            intent.setClass(getContext(), ProfileActivity.class);
            intent.putExtra("ID", identifier.toString());
            startActivity(intent);
        };
        adapter = new RecyclerViewAdapter(dummyList, listener);

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.SearchUpdated);

        // show online users
        search(SearchCommand.ONLINE_USERS);
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        Map userInfo = notification.userInfo;
        if (userInfo instanceof SearchCommand) {
            dummyList.response = (SearchCommand) userInfo;
        }
        reloadData();
    }

    private boolean search(String keywords) {
        dummyList.clearData();

        SearchCommand cmd = new SearchCommand(keywords);
        Messenger messenger = Messenger.getInstance();
        return messenger.sendCommand(cmd);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);

        View usersView = view.findViewById(R.id.search_user_list);
        // Set the adapter
        assert usersView instanceof RecyclerView : "recycler view error: " + usersView;
        bindRecyclerView((RecyclerView) usersView);

        searchView = view.findViewById(R.id.search_box);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.setIconified(true);
                searchView.setQueryHint(query);
                return search(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return view;
    }
}
