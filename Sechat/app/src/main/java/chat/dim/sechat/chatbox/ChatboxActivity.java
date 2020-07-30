package chat.dim.sechat.chatbox;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;

import chat.dim.ID;
import chat.dim.common.BackgroundThread;
import chat.dim.filesys.ExternalStorage;
import chat.dim.filesys.Paths;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;
import chat.dim.ui.image.ImagePickerActivity;

public class ChatboxActivity extends ImagePickerActivity {

    private ID identifier = null;

    ChatboxFragment chatboxFragment = null;

    public ChatboxActivity() {
        super();
        setCrop(false);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbox_activity);

        Facebook facebook = Facebook.getInstance();
        Amanuensis clerk = Amanuensis.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        identifier = facebook.getID(string);
        assert identifier != null : "ID error: " + string;
        Conversation chatBox = clerk.getConversation(identifier);

        if (savedInstanceState == null) {
            chatboxFragment = ChatboxFragment.newInstance(chatBox);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, chatboxFragment)
                    .commitNow();
        }

        if (identifier.isGroup()) {
            setTitle(chatBox.getName() + " (...)");
            // refresh group title in background
            BackgroundThread.run(() -> refreshTitle(chatBox));
        } else {
            setTitle(chatBox.getName());
        }
    }

    private void refreshTitle(Conversation chatBox) {
        Message msg = new Message();
        msg.obj = chatBox.getTitle();
        msgHandler.sendMessage(msg);
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.obj instanceof CharSequence) {
                setTitle((CharSequence) msg.obj);
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatbox_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.more) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ChatManageActivity.class);
            intent.putExtra("ID", identifier.toString());
            startActivity(intent);
        }
        return true;
    }

    //
    //  ImagePickerActivity
    //

    @Override
    protected String getTemporaryDirectory() {
        return Paths.appendPathComponent(ExternalStorage.root, "tmp");
    }

    @Override
    protected void fetchImage(Bitmap bitmap) {
        if (bitmap != null) {
            chatboxFragment.sendImage(bitmap);
        }
    }
}
