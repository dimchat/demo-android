package chat.dim.sechat.search.ui.search;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.SearchView;

import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.cpu.SearchCommandProcessor;
import chat.dim.model.Messenger;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.SearchCommand;
import chat.dim.sechat.R;

public class SearchFragment extends Fragment implements Observer {

    private SearchViewModel mViewModel;
    private UserArrayAdapter adapter;

    private SearchView searchView;
    private ListView userListView;

    public static SearchFragment newInstance() {
        return new SearchFragment();
    }

    public SearchFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, SearchCommandProcessor.SearchUpdated);
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            adapter.notifyDataSetChanged();
            scrollToBottom();
        }
    };

    @Override
    public void onReceiveNotification(Notification notification) {
        Map userInfo = notification.userInfo;
        if (userInfo instanceof SearchCommand) {
            mViewModel.updateSearchResult((SearchCommand) userInfo);
        }
        // OK
        Message msg = new Message();
        msgHandler.sendMessage(msg);
    }

    void scrollToBottom() {
        List users = mViewModel.getUsers();
        userListView.setSelection(users.size());
    }

    private List<ID> getUsers() {
        return mViewModel.getUsers();
    }

    private boolean search(String keywords) {
        SearchCommand cmd = new SearchCommand(keywords);
        Messenger messenger = Messenger.getInstance();
        return messenger.sendCommand(cmd);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);

        userListView = view.findViewById(R.id.search_user_list);

        searchView = view.findViewById(R.id.search_box);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchView.setIconified(true);
                return search(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SearchViewModel.class);

        adapter = new UserArrayAdapter(getContext(), R.layout.search_item, getUsers());
        userListView.setAdapter(adapter);
        scrollToBottom();
    }

}
