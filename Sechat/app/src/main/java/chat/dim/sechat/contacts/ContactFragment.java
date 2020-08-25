package chat.dim.sechat.contacts;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.Map;

import chat.dim.ID;
import chat.dim.network.StateMachine;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.sechat.MainActivity;
import chat.dim.sechat.R;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.sechat.search.SearchActivity;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.Alert;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.list.Listener;

public class ContactFragment extends ListFragment<ContactViewAdapter, ContactList> implements Observer {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.ContactsUpdated);

        dummyList = new ContactList();
        Listener listener = (Listener<ContactViewAdapter.ViewHolder>) viewHolder -> {
            ID identifier = viewHolder.item.getIdentifier();
            assert getContext() != null : "fragment context error";
            Intent intent = new Intent();
            intent.setClass(getContext(), ProfileActivity.class);
            intent.putExtra("ID", identifier.toString());
            startActivity(intent);
        };
        adapter = new ContactViewAdapter(dummyList, listener);

        reloadData();
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.ContactsUpdated);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.ContactsUpdated)) {
            reloadData();
        }
    }

    @Override
    public void reloadData() {
        // reload data in background
        BackgroundThreads.rush(super::reloadData);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.contacts_fragment, container, false);

        // Set the adapter
        assert view instanceof RecyclerView : "recycler view error: " + view;
        bindRecyclerView((RecyclerView) view);

        FragmentActivity activity = getActivity();
        assert activity != null : "should not happen";
        activity.setTitle(R.string.main_contacts);

        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.right_top_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        MainActivity activity = (MainActivity) getActivity();
        assert activity != null : "main activity not found";

        String serverState = activity.serverState;
        if (serverState != null && !serverState.equals(StateMachine.runningState)) {
            Alert.tips(activity, R.string.server_connecting);
            return false;
        }

        if (item.getItemId() == R.id.search_user) {
            Intent intent = new Intent();
            intent.setClass(activity, SearchActivity.class);
            startActivity(intent);
        }
        return true;
    }
}
