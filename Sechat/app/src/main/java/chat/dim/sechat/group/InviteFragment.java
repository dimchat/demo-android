package chat.dim.sechat.group;

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
import java.util.Map;
import java.util.Set;

import chat.dim.ID;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.crypto.SignKey;
import chat.dim.extension.GroupManager;
import chat.dim.model.Facebook;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.sechat.R;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.Alert;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.list.Listener;

public class InviteFragment extends ListFragment<CandidateViewAdapter, CandidateList> {

    private GroupViewModel mViewModel;
    private ID identifier;
    ID from;

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

        reloadData();
    }

    private final Set<ID> selected = new HashSet<>();

    void updateMembers() {
        if (selected.size() == 0) {
            return;
        }

        // save group name
        Facebook facebook = Facebook.getInstance();
        String oldName = facebook.getGroupName(identifier);
        String newName = groupName.getText().toString();
        if (oldName == null || !oldName.equals(newName)) {
            if (newName.length() > 0) {
                User user = facebook.getCurrentUser();
                assert user != null : "failed to get current user";
                SignKey key = facebook.getPrivateKeyForSignature(user.identifier);
                assert key != null : "failed to get private key: " + user.identifier;
                Profile profile = new Profile(identifier);
                profile.setName(newName);
                profile.sign(key);
                facebook.saveProfile(profile);
            }
        }

        // invite group member(s)
        GroupManager gm = new GroupManager(identifier);
        if (gm.invite(new ArrayList<>(selected))) {
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
