package chat.dim.sechat.contacts;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import chat.dim.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.list.Listener;

public class ContactFragment extends ListFragment<RecyclerViewAdapter, ContactList> {

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ContactFragment() {
        super();

        dummyList = new ContactList();
        Listener listener = (Listener<RecyclerViewAdapter.ViewHolder>) viewHolder -> {
            ID identifier = viewHolder.item.getIdentifier();
            assert getContext() != null : "fragment context error";
            Intent intent = new Intent();
            intent.setClass(getContext(), ProfileActivity.class);
            intent.putExtra("ID", identifier.toString());
            startActivity(intent);
        };
        adapter = new RecyclerViewAdapter(dummyList, listener);

        reloadData();
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
        activity.setTitle(R.string.contacts);

        return view;
    }
}
