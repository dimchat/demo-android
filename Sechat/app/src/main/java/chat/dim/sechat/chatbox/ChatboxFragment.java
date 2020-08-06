package chat.dim.sechat.chatbox;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import java.io.IOException;
import java.util.Map;

import chat.dim.Callback;
import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.User;
import chat.dim.database.Database;
import chat.dim.digest.MD5;
import chat.dim.format.Hex;
import chat.dim.model.Conversation;
import chat.dim.model.Messenger;
import chat.dim.network.FtpServer;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.Client;
import chat.dim.sechat.R;
import chat.dim.ui.image.Images;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.media.AudioPlayer;
import chat.dim.ui.media.AudioRecorder;

public class ChatboxFragment extends ListFragment<MessageViewAdapter, MessageList> implements Observer {

    private ChatboxViewModel mViewModel;

    private RecyclerView msgListView;

    private ImageButton switchButton;

    private Button recordButton;

    private EditText inputText;
    private ImageButton photoButton;

    private Conversation chatBox = null;

    public ChatboxFragment() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MessageUpdated);
    }

    public static ChatboxFragment newInstance(Conversation chatBox) {
        ChatboxFragment fragment = new ChatboxFragment();
        fragment.setConversation(chatBox);
        return fragment;
    }

    private void setConversation(Conversation conversation) {
        chatBox = conversation;

        dummyList = new MessageList(chatBox);
        adapter = new MessageViewAdapter(dummyList, null);

        //reloadData();
    }

    private void scrollToBottom() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext());
        linearLayoutManager.setStackFromEnd(true);
        linearLayoutManager.scrollToPositionWithOffset(adapter.getItemCount() - 1, Integer.MIN_VALUE);
        msgListView.setLayoutManager(linearLayoutManager);
    }

    @Override
    protected void onReloaded() {
        super.onReloaded();
        scrollToBottom();
    }

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
        reloadData();
    }

    //
    //  Send Message
    //

    private void sendMessage(InstantMessage iMsg) {
        // prepare to send
        Callback callback = (result, error) -> {
            // TODO: check sending status
        };
        Messenger messenger = Messenger.getInstance();
        if (!messenger.sendMessage(iMsg, callback)) {
            throw new RuntimeException("failed to send message: " + iMsg);
        }
    }

    private void sendContent(Content content) {
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
        sendMessage(iMsg);
    }

    private void send() {
        String text = inputText.getText().toString();
        if (text.length() == 0) {
            return;
        }
        Content content = new TextContent(text);
        sendContent(content);
        // sent OK
        inputText.setText("");
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
        byte[] jpeg = Images.jpeg(bitmap);
        String filename = Hex.encode(MD5.digest(jpeg)) + ".jpeg";
        FtpServer ftp = FtpServer.getInstance();
        ftp.saveImage(jpeg, filename);

        // thumbnail
        byte[] thumbnail = Images.thumbnail(bitmap);
        //ftp.saveThumbnail(thumbnail, filename);

        // add image data length & thumbnail into message content
        ImageContent content = new ImageContent(jpeg, filename);
        content.put("length", jpeg.length);
        content.setThumbnail(thumbnail);

        sendContent(content);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.chatbox_fragment, container, false);

        msgListView = view.findViewById(R.id.msgListView);
        bindRecyclerView(msgListView); // Set the adapter

        switchButton = view.findViewById(R.id.switchButton);

        recordButton = view.findViewById(R.id.recordVoice);

        inputText = view.findViewById(R.id.inputMsg);
        photoButton = view.findViewById(R.id.photoButton);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ChatboxViewModel.class);

        // TODO: Use the ViewModel

        dummyList.setViewModel(mViewModel);
        reloadData();

        switchButton.setOnClickListener(v -> {
            if (v.isActivated()) {
                v.setActivated(false);
                recordButton.setVisibility(View.GONE);
                inputText.setVisibility(View.VISIBLE);
            } else {
                v.setActivated(true);
                recordButton.setVisibility(View.VISIBLE);
                inputText.setVisibility(View.GONE);
            }
        });

        recordButton.setOnTouchListener((v, event) -> {
            Button btn = (Button) v;
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN: {
                    startRecord();
                    btn.setText(R.string.voice_record_stop);
                    break;
                }
                case MotionEvent.ACTION_UP: {
                    stopRecord();
                    btn.setText(R.string.voice_record_start);
                    v.performClick();
                    break;
                }
                default: {
                    break;
                }
            }
            return false;
        });

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

        // create audio player
        adapter.setAudioPlayer(new AudioPlayer(getActivity()));
    }

    @Override
    public void onDestroy() {
        // destroy audio player
        adapter.setAudioPlayer(null);
        super.onDestroy();
    }

    private AudioRecorder recorder = null;
    private final String mp4FilePath = Database.getTemporaryFilePath("audio.mp4");

    private void startRecord() {
        String path = mp4FilePath;
        if (Database.exists(path)) {
            try {
                Database.delete(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (recorder == null) {
            recorder = new AudioRecorder(getActivity());
        }
        recorder.startRecord(path);
    }
    private void stopRecord() {
        if (recorder == null) {
            return;
        }
        String path = recorder.stopRecord();
        if (path != null && Database.exists(path)) {
            try {
                byte[] mp4 = Database.loadData(path);
                sendVoice(mp4, recorder.getDuration());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        recorder = null;
    }

    private void sendVoice(byte[] mp4, int duration) {
        if (mp4 == null) {
            return;
        }
        String filename = Hex.encode(MD5.digest(mp4)) + ".mp4";
        String path = Database.getCacheFilePath(filename);
        if (!Database.exists(path)) {
            try {
                Database.saveData(mp4, path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        AudioContent content = new AudioContent(mp4, filename);
        content.put("duration", duration);
        sendContent(content);
    }
}
