package chat.dim.sechat.group;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import chat.dim.ID;
import chat.dim.common.BackgroundThread;
import chat.dim.extension.GroupManager;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.GroupCommand;
import chat.dim.sechat.R;
import chat.dim.ui.Alert;
import chat.dim.ui.list.Listener;

public class InviteFragment extends Fragment {

    private ParticipantViewModel mViewModel;
    private ID identifier;
    ID from;

    private ImageView groupLogo;
    private EditText groupName;
    private TextView groupOwner;

    private RecyclerView contacts;

    private CandidateList dummyList;
    private RecyclerViewAdapter adapter;

    public InviteFragment() {
        super();
    }

    public static InviteFragment newInstance(ID group) {
        InviteFragment fragment = new InviteFragment();
        fragment.setIdentifier(group);
        return fragment;
    }

    private void setIdentifier(ID group) {
        identifier = group;

        dummyList = new CandidateList(group);
        Listener listener = (Listener<RecyclerViewAdapter.ViewHolder>) viewHolder -> {
            boolean checked = viewHolder.checkBox.isChecked();
            if (checked) {
                viewHolder.checkBox.setChecked(false);
                selected.remove(viewHolder.item.getIdentifier());
            } else {
                viewHolder.checkBox.setChecked(true);
                selected.add(viewHolder.item.getIdentifier());
            }
        };
        adapter = new RecyclerViewAdapter(dummyList, listener);

        reloadData();
    }

    private final Set<ID> selected = new HashSet<>();

    void updateMembers() {
        if (selected.size() == 0) {
            return;
        }

        GroupManager gm = new GroupManager(identifier);
        //noinspection unchecked
        if (gm.invite(new ArrayList(selected))) {
            Map<String, Object> info = new HashMap<>();
            info.put("ID", identifier);
            info.put("from", from);
            info.put("command", GroupCommand.INVITE);
            info.put("members", selected);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.MembersUpdated, this, info);

            Alert.tips(getContext(), R.string.group_members_updated);
            getActivity().finish();
        } else {
            Alert.tips(getContext(), R.string.group_members_error);
        }
    }

    public void reloadData() {
        BackgroundThread.run(() -> {
            dummyList.reloadData();
            msgHandler.sendMessage(new Message());
        });
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            adapter.notifyDataSetChanged();
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.invite_fragment, container, false);

        groupLogo = view.findViewById(R.id.logo);
        groupName = view.findViewById(R.id.name);
        groupOwner = view.findViewById(R.id.owner);

        contacts = view.findViewById(R.id.contacts);
        contacts.setLayoutManager(new LinearLayoutManager(getContext()));
        contacts.setAdapter(adapter);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ParticipantViewModel.class);
        mViewModel.setGroup(identifier);

        // TODO: Use the ViewModel

        groupLogo.setImageBitmap(mViewModel.getLogo());
        groupName.setText(mViewModel.getName());
        groupOwner.setText(mViewModel.getOwnerName());
    }
}