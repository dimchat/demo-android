package chat.dim.sechat.chatbox;

import android.annotation.SuppressLint;
import androidx.lifecycle.ViewModelProviders;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
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
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.User;
import chat.dim.digest.MD5;
import chat.dim.filesys.ExternalStorage;
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
import chat.dim.sechat.SechatApp;
import chat.dim.stargate.StarShip;
import chat.dim.ui.OnKeyboardListener;
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

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.MessageUpdated);
        // destroy audio player
        adapter.setAudioPlayer(null);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MessageUpdated)) {
            ID entity = (ID) info.get("ID");
            if (chatBox.identifier.equals(entity)) {
                reloadData();
            }
        }
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
    }

    private void scrollToBottom() {
        int count = adapter.getItemCount();
        if (count > 0) {
            //msgListView.smoothScrollToPosition(count - 1);
            msgListView.scrollToPosition(count - 1);
        }
    }

    @Override
    protected void onReloaded() {
        super.onReloaded();
        scrollToBottom();
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
        if (!messenger.sendMessage(iMsg, callback, StarShip.NORMAL)) {
            throw new RuntimeException("failed to send message: " + iMsg);
        }
    }

    private void sendContent(chat.dim.protocol.Content content) {
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
        InstantMessage iMsg = new InstantMessage<>(content, sender, receiver);
        sendMessage(iMsg);
    }

    private void send() {
        String text = inputText.getText().toString();
        if (text.length() == 0) {
            return;
        }
        sendContent(new TextContent(text));
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

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ChatboxViewModel.class);

        mViewModel.setIdentifier(chatBox.identifier);
        mViewModel.refreshProfile();

        dummyList.setViewModel(mViewModel);
        reloadData();

        SechatApp.getInstance().setKeyboardListener(getActivity(), new OnKeyboardListener() {
            @Override
            public void onKeyboardShown() {
                scrollToBottom();
            }

            @Override
            public void onKeyboardHidden() {
            }
        });

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
                    btn.performClick();
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

    private AudioRecorder recorder = null;
    private final String mp4FilePath = getTemporaryFilePath();

    private static String getTemporaryFilePath() {
        try {
            return ExternalStorage.getTemporaryFilePath("audio.mp4");
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private void startRecord() {
        String path = mp4FilePath;
        if (ExternalStorage.exists(path)) {
            try {
                ExternalStorage.delete(path);
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
        if (path != null && ExternalStorage.exists(path)) {
            try {
                byte[] mp4 = ExternalStorage.loadData(path);
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
        try {
            String path = ExternalStorage.getCacheFilePath(filename);
            ExternalStorage.saveData(mp4, path);
        } catch (IOException e) {
            e.printStackTrace();
        }

        AudioContent content = new AudioContent(mp4, filename);
        content.put("duration", duration);
        sendContent(content);
    }
}
