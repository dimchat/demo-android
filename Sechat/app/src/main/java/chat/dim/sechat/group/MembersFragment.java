package chat.dim.sechat.group;

import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.sechat.R;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.sechat.profile.ProfileViewModel;

public class MembersFragment extends Fragment {

    private GroupViewModel mViewModel;
    private ParticipantsAdapter adapter;

    private GridView participantsView;

    ID identifier = null;
    private List<ID> participants = null;

    public static MembersFragment newInstance(ID identifier) {
        if (identifier.isGroup()) {
            GroupViewModel.checkMembers(identifier);
            // refresh group profile
            ProfileViewModel.updateProfile(identifier);
        }

        MembersFragment fragment = new MembersFragment();
        fragment.identifier = identifier;
        return fragment;
    }

    private boolean isGroupAdmin() {
        if (identifier != null && identifier.isGroup()) {
            User user = UserViewModel.getCurrentUser();
            return GroupViewModel.isAdmin(user, identifier);
        }
        return false;
    }
    private List<ID> getParticipants() {

        List<ID> members = mViewModel.getMembers();
        List<ID> participants = new ArrayList<>(members);

        // "invite member"
        participants.add(ParticipantsAdapter.INVITE_BTN_ID);

        // "expel member"
        if (isGroupAdmin()) {
            participants.add(ParticipantsAdapter.EXPEL_BTN_ID);
        }

        return participants;
    }
    void reloadData() {
        participants = getParticipants();
        adapter.notifyDataSetChanged();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.members_fragment, container, false);

        participantsView = view.findViewById(R.id.participants);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(GroupViewModel.class);
        mViewModel.setIdentifier(identifier);

        // TODO: Use the ViewModel

        // participants
        participants = getParticipants();
        adapter = new ParticipantsAdapter(getContext(), R.layout.participants_grid_item, participants, identifier);
        participantsView.setAdapter(adapter);
    }
}
