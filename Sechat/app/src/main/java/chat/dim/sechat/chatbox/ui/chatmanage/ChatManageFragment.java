package chat.dim.sechat.chatbox.ui.chatmanage;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.TextView;

import java.util.List;

import chat.dim.ID;
import chat.dim.sechat.R;

public class ChatManageFragment extends Fragment {

    private ChatManageViewModel mViewModel;
    private ParticipantsAdapter adapter;

    private GridView participantsView;

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

        nameTextView = view.findViewById(R.id.nameView);
        seedTextView = view.findViewById(R.id.seedView);
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
        adapter = new ParticipantsAdapter(getContext(), R.layout.chat_manage_participant, participants);
        participantsView.setAdapter(adapter);

        nameTextView.setText(mViewModel.getName(identifier));
        seedTextView.setText(identifier.name);
        addressTextView.setText(identifier.address.toString());
        numberTextView.setText(mViewModel.getNumberString(identifier));
    }

}
