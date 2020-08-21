package chat.dim.sechat.chatbox;

import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;

import java.util.List;

import chat.dim.ID;
import chat.dim.User;
import chat.dim.model.Facebook;
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
    private Button quitGroupButton;

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

        quitGroupButton = view.findViewById(R.id.quitGroup);
        if (canQuit()) {
            quitGroupButton.setOnClickListener(v -> quitGroup());
        } else {
            quitGroupButton.setVisibility(View.GONE);
        }

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

    private void showMembers() {
        Context context = getContext();
        assert context != null : "failed to get fragment context";
        Intent intent = new Intent();
        intent.setClass(context, MembersActivity.class);
        intent.putExtra("ID", identifier.toString());
        getContext().startActivity(intent);
    }

    private void clearHistory() {
        if (mViewModel.clearHistory(identifier)) {
            Alert.tips(getActivity(), R.string.clear_history_ok);
            close();
        }
    }

    private boolean canQuit() {
        if (!identifier.isGroup()) {
            return false;
        }
        Facebook facebook = Facebook.getInstance();
        User user = facebook.getCurrentUser();
        assert user != null : "failed to get current user";
        boolean isMember = facebook.existsMember(user.identifier, identifier);
        boolean isOwner = facebook.isOwner(user.identifier, identifier);
        // TODO: isAdmin? isAssistant?
        return isMember && !isOwner;
    }

    private void quitGroup() {
        if (mViewModel.quitGroup(identifier)) {
            Alert.tips(getActivity(), R.string.group_quit_ok);
            close();
        }
    }

    private void close() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            // should not happen
            return;
        }
        activity.finish();
    }
}
