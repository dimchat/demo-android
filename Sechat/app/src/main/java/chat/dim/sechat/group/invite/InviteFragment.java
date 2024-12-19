package chat.dim.sechat.group.invite;

import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.crypto.SignKey;
import chat.dim.group.SharedGroupManager;
import chat.dim.mkm.BaseBulletin;
import chat.dim.mkm.User;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.group.CandidateList;
import chat.dim.sechat.group.CandidateViewAdapter;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.Alert;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.list.Listener;

public class InviteFragment extends ListFragment<CandidateViewAdapter, CandidateList> {

    private GroupViewModel mViewModel = null;
    private ID identifier = null;
    private ID from = null;

    private ImageView groupLogo;
    private EditText groupName;
    private TextView groupOwner;

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
        Listener listener = (Listener<CandidateViewAdapter.ViewHolder>) viewHolder -> {
            if (!viewHolder.checkBox.isEnabled()) {
                return;
            }
            boolean checked = viewHolder.checkBox.isChecked();
            if (checked) {
                viewHolder.checkBox.setChecked(false);
                selected.remove(viewHolder.item.getIdentifier());
            } else {
                viewHolder.checkBox.setChecked(true);
                selected.add(viewHolder.item.getIdentifier());
            }
        };
        adapter = new CandidateViewAdapter(dummyList, listener);
        adapter.group = group;

        BackgroundThreads.rush(this::reloadData);
    }

    void setFrom(ID identifier) {
        from = identifier;
        if (identifier.isUser()) {
            adapter.from = identifier;
            selected.add(identifier);
        }
    }

    private final Set<ID> selected = new HashSet<>();

    void updateMembers() {
        if (selected.size() == 0) {
            return;
        }

        // save group name
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        String oldName = facebook.getName(identifier);
        String newName = groupName.getText().toString();
        if (oldName == null || !oldName.equals(newName)) {
            if (newName.length() > 0) {
                User user = facebook.getCurrentUser();
                assert user != null : "failed to get current user";
                SignKey sKey = facebook.getPrivateKeyForVisaSignature(user.getIdentifier());
                assert sKey != null : "failed to get private key: " + user.getIdentifier();
                Bulletin bulletin = new BaseBulletin(identifier);
                bulletin.setName(newName);
                bulletin.sign(sKey);
                facebook.saveDocument(bulletin);
            }
        }

        // invite group member(s)
        List<ID> members = new ArrayList<>(selected);
        SharedGroupManager manager = SharedGroupManager.getInstance();
        if (manager.inviteGroupMembers(members, identifier)) {
            if (from.isUser()) {
                Map<String, Object> info = new HashMap<>();
                info.put("ID", identifier);
                info.put("from", from);
                info.put("members", selected);
                NotificationCenter nc = NotificationCenter.getInstance();
                nc.postNotification(NotificationNames.GroupCreated, this, info);
            }

            Alert.tips(getContext(), R.string.group_members_updated);
            close();
        } else {
            Alert.tips(getContext(), R.string.group_members_error);
        }
    }

    @Override
    public void reloadData() {
        // reload data in background
        BackgroundThreads.rush(super::reloadData);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.invite_fragment, container, false);

        RecyclerView contacts = view.findViewById(R.id.contacts);
        bindRecyclerView(contacts); // Set the adapter

        groupLogo = view.findViewById(R.id.logo);
        groupName = view.findViewById(R.id.name);
        groupOwner = view.findViewById(R.id.owner);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        mViewModel.setIdentifier(identifier);

        // TODO: Use the ViewModel

        groupLogo.setImageBitmap(mViewModel.getLogo());
        groupName.setText(mViewModel.getName());
        groupOwner.setText(mViewModel.getOwnerName());
    }
}
