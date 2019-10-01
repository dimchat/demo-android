package chat.dim.sechat.chatbox.ui.chatbox;

import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;

import chat.dim.common.Conversation;
import chat.dim.common.Messenger;
import chat.dim.core.Callback;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.Client;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ChatboxActivity;

public class ChatboxFragment extends Fragment {

    private ChatboxViewModel mViewModel;
    private MessageArrayAdapter adapter;

    private ListView msgListView;
    private EditText inputText;
    private Button sendButton;

    public static ChatboxFragment newInstance() {
        return new ChatboxFragment();
    }

    private Conversation getConversation() {
        ChatboxActivity activity = (ChatboxActivity) getActivity();
        assert activity != null;
        return activity.chatBox;
    }

    private List<InstantMessage> getMessages() {
        Conversation chatBox = getConversation();
        if (chatBox == null) {
            return null;
        }
        return mViewModel.getMessages(chatBox);
    }

    private InstantMessage sendText(String text) {
        Client client = Client.getInstance();
        LocalUser user = client.getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user cannot be empty");
        }
        Conversation chatBox = getConversation();
        if (chatBox == null) {
            throw new NullPointerException("conversation ID should not be empty");
        }
        // pack message content
        ID sender = user.identifier;
        ID receiver = chatBox.identifier;
        Content content = new TextContent(text);
        if (receiver.getType().isGroup()) {
            content.setGroup(receiver);
        }
        InstantMessage iMsg = new InstantMessage(content, sender, receiver);
        // prepare to send
        Callback callback = new Callback() {
            @Override
            public void onFinished(Object result, Error error) {
                // TODO:
            }
        };
        Messenger messenger = Messenger.getInstance();
        if (!messenger.sendMessage(iMsg, callback, true)) {
            throw new RuntimeException("failed to send message: " + iMsg);
        }
        return iMsg;
    }

    private boolean send(String text) {
        if (text.length() == 0) {
            return false;
        }
        InstantMessage iMsg = sendText(text);
        if (iMsg == null) {
            return false;
        }
        mViewModel.insertMessage(iMsg, getConversation());
        List messages = getMessages();
        adapter.notifyDataSetChanged();
        msgListView.setSelection(messages.size());
        return true;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatbox_fragment, container, false);

        msgListView = (ListView) view.findViewById(R.id.msgListView);
        inputText = (EditText) view.findViewById(R.id.inputMsg);
        sendButton = (Button) view.findViewById(R.id.sendMsg);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = inputText.getText().toString();
                if (send(text)) {
                    // sent OK
                    inputText.setText("");
                }
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ChatboxViewModel.class);

        adapter = new MessageArrayAdapter(getContext(), R.layout.chatbox_message, getMessages());
        adapter.chatBox = getConversation();
        msgListView.setAdapter(adapter);
    }
}
