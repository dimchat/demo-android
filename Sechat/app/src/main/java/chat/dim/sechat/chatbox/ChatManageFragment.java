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
import chat.dim.sechat.R;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.ui.Alert;

public class ChatManageFragment extends Fragment {

    public static int MAX_PARTICIPANTS_SHOWN = 2 * 3 * 5 - 1;

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
        View view = inflater.inflate(R.layout.chat_manage_fragment, container, false);

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

        // participants
        List<ID> participants = mViewModel.getParticipants(identifier);
        if (participants.size() > MAX_PARTICIPANTS_SHOWN) {
            participants = participants.subList(0, MAX_PARTICIPANTS_SHOWN);
            showMembersButton.setVisibility(View.VISIBLE);
        } else {
            showMembersButton.setVisibility(View.GONE);
        }
        participants.add(ID.ANYONE);

        adapter = new ParticipantsAdapter(getContext(), R.layout.chat_manage_participant, participants);
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
