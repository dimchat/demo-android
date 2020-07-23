package chat.dim.sechat.chatbox;

import android.annotation.SuppressLint;
import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import java.util.List;
import java.util.Map;

import chat.dim.Callback;
import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.User;
import chat.dim.digest.MD5;
import chat.dim.format.Hex;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Messenger;
import chat.dim.network.FtpServer;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.Client;
import chat.dim.sechat.R;
import chat.dim.ui.Images;

public class ChatboxFragment extends Fragment implements Observer {

    private ChatboxViewModel mViewModel;
    private MessageArrayAdapter adapter;

    private ListView msgListView;
    private EditText inputText;
    private ImageButton photoButton;

    private Conversation chatBox = null;

    public static ChatboxFragment newInstance(Conversation chatBox) {
        ChatboxFragment fragment = new ChatboxFragment();
        fragment.chatBox = chatBox;
        return fragment;
    }

    public ChatboxFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, ConversationDatabase.MessageUpdated);
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            adapter.notifyDataSetChanged();
            scrollToBottom();
        }
    };

    @Override
    public void onReceiveNotification(Notification notification) {
        Map userInfo = notification.userInfo;
        if (userInfo == null) {
            return;
        }
        ID identifier = (ID) userInfo.get("ID");
        if (identifier == null || !identifier.equals(chatBox.identifier)) {
            return;
        }
        // OK
        Message msg = new Message();
        msgHandler.sendMessage(msg);
    }

    void scrollToBottom() {
        List messages = mViewModel.getMessages(chatBox);
        msgListView.setSelection(messages.size());
    }

    private List<InstantMessage> getMessages() {
        if (chatBox == null) {
            return null;
        }
        return mViewModel.getMessages(chatBox);
    }

    //
    //  Send Message
    //

    private InstantMessage sendMessage(InstantMessage iMsg) {
        // prepare to send
        Callback callback = (result, error) -> {
            // TODO: check sending status
        };
        Messenger messenger = Messenger.getInstance();
        if (!messenger.sendMessage(iMsg, callback)) {
            throw new RuntimeException("failed to send message: " + iMsg);
        }

        return iMsg;
    }

    private InstantMessage sendContent(Content content) {
        Client client = Client.getInstance();
        User user = client.getCurrentUser();
        if (user == null) {
            throw new NullPointerException("current user cannot be empty");
        }
        if (chatBox == null) {
            throw new NullPointerException("conversation ID should not be empty");
        }
        // pack message content
        ID sender = user.identifier;
        ID receiver = chatBox.identifier;
        if (receiver.isGroup()) {
            content.setGroup(receiver);
        }
        InstantMessage iMsg = new InstantMessage(content, sender, receiver);
        return sendMessage(iMsg);
    }

    private void send() {
        String text = inputText.getText().toString();
        if (text.length() == 0) {
            return;
        }
        Content content = new TextContent(text);
        InstantMessage iMsg = sendContent(content);
        if (iMsg != null) {
            // sent OK
            inputText.setText("");
        }
    }

    private static final Images.Size MAX_SIZE = new Images.Size(1024, 1024);

    void sendImage(Bitmap bitmap) {

        // check image size
        Images.Size size = Images.getSize(bitmap);
        if (size.width > MAX_SIZE.width || size.height > MAX_SIZE.height) {
            size = Images.aspectFit(size, MAX_SIZE);
            bitmap = Images.scale(bitmap, size);
        }

        // image file
        byte[] imageData = Images.jpeg(bitmap);
        String filename = Hex.encode(MD5.digest(imageData)) + ".jpeg";
        FtpServer ftp = FtpServer.getInstance();
        ftp.saveImage(imageData, filename);

        // thumbnail
        byte[] thumbnail = Images.thumbnail(bitmap);
        //ftp.saveThumbnail(thumbnail, filename);

        // add image data length & thumbnail into message content
        byte[] jpeg = Images.jpeg(bitmap);
        ImageContent content = new ImageContent(jpeg, "photo.jpeg");
        content.setFilename(filename);
        content.put("length", imageData.length);
        content.setThumbnail(thumbnail);

        sendContent(content);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatbox_fragment, container, false);

        msgListView = view.findViewById(R.id.msgListView);
        inputText = view.findViewById(R.id.inputMsg);
        photoButton = view.findViewById(R.id.photoButton);

        inputText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                send();
                return true;
            }
            return false;
        });
        inputText.setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                send();
                return true;
            }
            return false;
        });

        photoButton.setOnClickListener(v -> {
            ChatboxActivity activity = (ChatboxActivity) getActivity();
            assert activity != null : "chatbox activity error";
            activity.startImagePicker();
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ChatboxViewModel.class);

        adapter = new MessageArrayAdapter(getContext(), R.layout.chatbox_message, getMessages());
        adapter.chatBox = chatBox;
        msgListView.setAdapter(adapter);
        scrollToBottom();
    }
}
