package chat.dim.sechat.chatbox.manage;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import java.util.Map;

import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.threading.MainThread;

public class ChatManageActivity extends AppCompatActivity implements Observer {

    private ChatManageFragment fragment = null;

    public ChatManageActivity() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.GroupCreated);
        nc.addObserver(this, NotificationNames.MembersUpdated);
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.GroupCreated);
        nc.removeObserver(this, NotificationNames.MembersUpdated);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.MembersUpdated)) {
            ID group = (ID) info.get("group");
            if (fragment.identifier.equals(group)) {
                Amanuensis clerk = Amanuensis.getInstance();
                Conversation chatBox = clerk.getConversation(fragment.identifier);
                String title = chatBox.getTitle();
                MainThread.call(() -> setTitle(title));
                fragment.reloadData();
            }
        } else if (name.equals(NotificationNames.GroupCreated)) {
            ID from = (ID) info.get("from");
            if (fragment.identifier.equals(from)) {
                finish();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatman_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Amanuensis clerk = Amanuensis.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = ID.parse(string);
        assert identifier != null : "ID error: " + string;
        Conversation chatBox = clerk.getConversation(identifier);
        setTitle(chatBox.getTitle());

        if (savedInstanceState == null) {
            fragment = ChatManageFragment.newInstance(identifier);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commitNow();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
