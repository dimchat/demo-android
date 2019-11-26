package chat.dim.sechat.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import chat.dim.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.list.Listener;

public class ContactFragment extends ListFragment<RecyclerViewAdapter, DummyContent> {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactFragment() {
        super();

        dummyList = new DummyContent();
        Listener listener = (Listener<DummyContent.Item>) item -> {
            ID identifier = item.getIdentifier();
            assert getContext() != null;
            Intent intent = new Intent();
            intent.setClass(getContext(), ProfileActivity.class);
            intent.putExtra("ID", identifier.toString());
            startActivity(intent);
        };
        adapter = new RecyclerViewAdapter(dummyList, listener);

//        NotificationCenter nc = NotificationCenter.getInstance();
//        nc.addObserver(this, ConversationDatabase.MessageUpdated);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_contact_list, container, false);

        // Set the adapter
        assert view instanceof RecyclerView;
        bindRecyclerView((RecyclerView) view);

        return view;
    }
}
