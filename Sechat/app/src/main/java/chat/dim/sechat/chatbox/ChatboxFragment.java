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

import chat.dim.GlobalVariable;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.LocalCache;
import chat.dim.filesys.Paths;
import chat.dim.model.Conversation;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.threading.BackgroundThreads;
import chat.dim.ui.OnKeyboardListener;
import chat.dim.ui.image.Images;
import chat.dim.ui.list.ListFragment;
import chat.dim.ui.media.AudioPlayer;
import chat.dim.ui.media.AudioRecorder;
import chat.dim.utils.Log;

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

    private void send() {
        String text = inputText.getText().toString();
        if (text.length() > 0) {
            GlobalVariable shared = GlobalVariable.getInstance();
            shared.emitter.sendText(text, chatBox.identifier);
        }
        // clear text after sent
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
        // thumbnail
        byte[] thumbnail = Images.thumbnail(bitmap);

        GlobalVariable shared = GlobalVariable.getInstance();
        shared.emitter.sendImage(jpeg, thumbnail, chatBox.identifier);
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
        mViewModel.refreshDocument();

        dummyList.setViewModel(mViewModel);
        BackgroundThreads.rush(this::reloadData);

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
        LocalCache cache = LocalCache.getInstance();
        String dir = cache.getTemporaryDirectory();
        return Paths.append(dir, "audio.mp4");
    }

    private void startRecord() {
        String path = mp4FilePath;
        Paths.delete(path);

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
        recorder = null;
        if (path == null || !Paths.exists(path)) {
            Log.error("voice file not found: " + path);
            return;
        }
        try {
            byte[] mp4 = ExternalStorage.loadBinary(path);
            GlobalVariable shared = GlobalVariable.getInstance();
            shared.emitter.sendVoice(mp4, recorder.getDuration(), chatBox.identifier);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
