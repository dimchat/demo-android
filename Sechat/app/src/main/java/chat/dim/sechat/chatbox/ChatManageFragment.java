package chat.dim.sechat.chatbox;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.util.List;

import chat.dim.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.group.MembersActivity;
import chat.dim.sechat.group.ParticipantsAdapter;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.model.GroupViewModel;
import chat.dim.sechat.profile.ProfileViewModel;
import chat.dim.ui.Alert;

public class ChatManageFragment extends Fragment {

    private ChatManageViewModel mViewModel;
    private ParticipantsAdapter adapter;

    private GridView participantsView;

    private Button showMembersButton;
    private Button clearHistoryButton;

    private TextView nameTextView;
    private TextView addressTextView;
    private TextView numberTextView;

    ID identifier = null;
    private List<ID> participants = null;

    public static ChatManageFragment newInstance(ID identifier) {
        if (identifier.isGroup()) {
            GroupViewModel.checkMembers(identifier);
            // refresh group profile
            ProfileViewModel.updateProfile(identifier);
        }

        ChatManageFragment fragment = new ChatManageFragment();
        fragment.identifier = identifier;
        return fragment;
    }

    private List<ID> getParticipants() {

        int maxItemCount = mViewModel.getMaxItemCount();
        participants = mViewModel.getParticipants(maxItemCount);

        // "show members"
        if (participants.size() < maxItemCount) {
            showMembersButton.setVisibility(View.GONE);
        } else {
            showMembersButton.setVisibility(View.VISIBLE);
        }

        // "invite member"
        participants.add(ParticipantsAdapter.INVITE_BTN_ID);

        // "expel member"
        if (mViewModel.isGroupAdmin()) {
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
        View view = inflater.inflate(R.layout.chatman_fragment, container, false);

        participantsView = view.findViewById(R.id.participants);

        showMembersButton = view.findViewById(R.id.showMembers);
        showMembersButton.setOnClickListener(v -> showMembers());

        clearHistoryButton = view.findViewById(R.id.clearHistory);
        clearHistoryButton.setOnClickListener(v -> clearHistory());

        nameTextView = view.findViewById(R.id.nameView);
        addressTextView = view.findViewById(R.id.addressView);
        numberTextView = view.findViewById(R.id.numberView);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ChatManageViewModel.class);
        mViewModel.setIdentifier(identifier);

        // participants
        participants = getParticipants();
        adapter = new ParticipantsAdapter(getContext(), R.layout.participants_grid_item, participants, identifier);
        participantsView.setAdapter(adapter);

        nameTextView.setText(EntityViewModel.getName(identifier));
        addressTextView.setText(identifier.address.toString());
        numberTextView.setText(EntityViewModel.getNumberString(identifier));
    }

    @Override
    public void onDestroy() {
        adapter = null;
        mViewModel = null;
        super.onDestroy();
    }

    private void showMembers() {
        Intent intent = new Intent();
        intent.setClass(getContext(), MembersActivity.class);
        intent.putExtra("ID", identifier.toString());
        getContext().startActivity(intent);
    }

    private void clearHistory() {
        if (mViewModel.clearHistory(identifier)) {
            Alert.tips(getActivity(), R.string.clear_history_ok);
        }
    }
}
