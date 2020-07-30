package chat.dim.sechat.chatbox;

import android.arch.lifecycle.ViewModelProviders;
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
import chat.dim.User;
import chat.dim.sechat.Client;
import chat.dim.sechat.R;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.ui.Alert;

public class ChatManageFragment extends Fragment {

    public static int MAX_ITEM_COUNT = 2 * 3 * 5;

    private ChatManageViewModel mViewModel;
    private ParticipantsAdapter adapter;

    private GridView participantsView;

    private Button showMembersButton;
    private Button clearHistoryButton;

    private TextView nameTextView;
    private TextView seedTextView;
    private TextView addressTextView;
    private TextView numberTextView;

    private ID identifier = null;

    public static ChatManageFragment newInstance(ID identifier) {
        ChatManageFragment fragment = new ChatManageFragment();
        fragment.identifier = identifier;
        return fragment;
    }

    public ChatManageFragment() {
        super();
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
        seedTextView = view.findViewById(R.id.nameView);
        addressTextView = view.findViewById(R.id.addressView);
        numberTextView = view.findViewById(R.id.numberView);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ChatManageViewModel.class);

        boolean isGroupAdmin = false;
        if (identifier.isGroup()) {
            Client client = Client.getInstance();
            User user = client.getCurrentUser();
            if (ChatManageViewModel.isAdmin(user, identifier)) {
                isGroupAdmin = true;
            }
        }

        // participants
        List<ID> participants = mViewModel.getParticipants(identifier);
        int count = participants.size();
        if (isGroupAdmin) {
            if (count > (MAX_ITEM_COUNT - 2)) {
                participants = participants.subList(0, MAX_ITEM_COUNT - 2);
                showMembersButton.setVisibility(View.VISIBLE);
            } else {
                showMembersButton.setVisibility(View.GONE);
            }
            participants.add(ChatManageViewModel.INVITE_BTN_ID);
            participants.add(ChatManageViewModel.EXPEL_BTN_ID);
        } else {
            if (participants.size() > (MAX_ITEM_COUNT - 1)) {
                participants = participants.subList(0, MAX_ITEM_COUNT - 1);
                showMembersButton.setVisibility(View.VISIBLE);
            } else {
                showMembersButton.setVisibility(View.GONE);
            }
            participants.add(ChatManageViewModel.INVITE_BTN_ID);
        }

        adapter = new ParticipantsAdapter(getContext(), R.layout.chatman_participant, participants, identifier);
        participantsView.setAdapter(adapter);

        nameTextView.setText(EntityViewModel.getName(identifier));
        seedTextView.setText(identifier.name);
        addressTextView.setText(identifier.address.toString());
        numberTextView.setText(EntityViewModel.getNumberString(identifier));
    }

    private void showMembers() {
        Alert.tips(getActivity(), "not implemented yet");
    }

    private void clearHistory() {
        if (mViewModel.clearHistory(identifier)) {
            Alert.tips(getActivity(), R.string.clear_history_ok);
        }
    }
}
