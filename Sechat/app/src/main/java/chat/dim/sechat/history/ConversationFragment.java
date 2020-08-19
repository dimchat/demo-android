package chat.dim.sechat.history;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import chat.dim.ID;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ChatboxActivity;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.list.Listener;

public class ConversationFragment extends ListFragment<ConversationViewAdapter, ConversationList> implements Observer {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ConversationFragment() {
        super();

        dummyList = new ConversationList();
        Listener listener = (Listener<ConversationViewAdapter.ViewHolder>) viewHolder -> {
            ID identifier = viewHolder.item.getIdentifier();
            assert getContext() != null : "fragment context error";
            Intent intent = new Intent();
            intent.setClass(getContext(), ChatboxActivity.class);
            intent.putExtra("ID", identifier.toString());
            startActivity(intent);
        };
        adapter = new ConversationViewAdapter(dummyList, listener);

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MessageUpdated);

        reloadData();
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.MessageUpdated);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        assert name != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MessageUpdated)) {
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
        View view = inflater.inflate(R.layout.conversations_fragment, container, false);

        // Set the adapter
        assert view instanceof RecyclerView : "recycler view error: " + view;
        bindRecyclerView((RecyclerView) view);

        FragmentActivity activity = getActivity();
        assert activity != null : "should not happen";
        activity.setTitle(R.string.app_name);

        return view;
    }
}
