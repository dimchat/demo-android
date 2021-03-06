package chat.dim.sechat.chatbox;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import androidx.appcompat.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;

import java.io.IOException;
import java.util.Map;

import chat.dim.filesys.ExternalStorage;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;
import chat.dim.sechat.chatbox.manage.ChatManageActivity;
import chat.dim.threading.BackgroundThreads;
import chat.dim.threading.MainThread;
import chat.dim.ui.image.ImagePickerActivity;

public class ChatboxActivity extends ImagePickerActivity implements Observer {

    private ID identifier = null;

    ChatboxFragment chatboxFragment = null;

    public ChatboxActivity() {
        super();
        setCrop(false);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MembersUpdated);
        nc.addObserver(this, NotificationNames.GroupCreated);
        nc.addObserver(this, NotificationNames.GroupRemoved);
    }

    @Override
    public void onDestroy() {
        SechatApp.getInstance().clearKeyboardListeners();

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.MembersUpdated);
        nc.removeObserver(this, NotificationNames.GroupCreated);
        nc.removeObserver(this, NotificationNames.GroupRemoved);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MembersUpdated)) {
            ID group = (ID) info.get("group");
            if (identifier.equals(group)) {
                refresh();
            }
        } else if (name.equals(NotificationNames.GroupCreated)) {
            ID from = (ID) info.get("from");
            if (identifier.equals(from)) {
                finish();
            }
        } else if (name.equals(NotificationNames.GroupRemoved)) {
            ID group = (ID) info.get("group");
            if (identifier.equals(group)) {
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbox_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Amanuensis clerk = Amanuensis.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        identifier = ID.parse(string);
        assert identifier != null : "ID error: " + string;
        Conversation chatBox = clerk.getConversation(identifier);
        assert chatBox != null : "chat bot error: " + identifier;

        if (savedInstanceState == null) {
            chatboxFragment = ChatboxFragment.newInstance(chatBox);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, chatboxFragment)
                    .commitNow();
        }

        setTitle(chatBox.getTitle());
        if (identifier.isGroup()) {
            // refresh group title in background
            BackgroundThreads.rush(this::refresh);
        }
    }

    private void refresh() {
        Amanuensis clerk = Amanuensis.getInstance();
        Conversation chatBox = clerk.getConversation(identifier);
        assert chatBox != null : "chat bot error: " + identifier;
        String title = chatBox.getTitle();
        MainThread.call(() -> setTitle(title));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatbox_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.more: {
                Intent intent = new Intent();
                intent.setClass(getApplicationContext(), ChatManageActivity.class);
                intent.putExtra("ID", identifier.toString());
                startActivity(intent);
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }

    //
    //  ImagePickerActivity
    //

    @Override
    protected String getTemporaryDirectory() throws IOException {
        return ExternalStorage.getTemporaryDirectory();
    }

    @Override
    protected void fetchImage(Bitmap bitmap) {
        if (bitmap != null) {
            chatboxFragment.sendImage(bitmap);
        }
    }
}
