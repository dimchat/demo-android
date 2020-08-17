package chat.dim.sechat.chatbox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.Map;

import chat.dim.ID;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.Facebook;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.sechat.R;
import chat.dim.sechat.model.GroupViewModel;

public class ChatManageActivity extends AppCompatActivity implements Observer {

    private ChatManageFragment fragment = null;

    public ChatManageActivity() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.MembersUpdated);
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.MembersUpdated);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        if (name == null || !name.equals(NotificationNames.MembersUpdated)) {
            return;
        }
        Map info = notification.userInfo;
        ID from = (ID) info.get("from");
        if (from == null) {
            from = (ID) info.get("ID");
        }
        if (from == null || !from.equals(fragment.identifier)) {
            return;
        }
        if (from.isUser()) {
            finish();
        } else {
            GroupViewModel.refreshLogo(fragment.identifier);

            Amanuensis clerk = Amanuensis.getInstance();
            Conversation chatBox = clerk.getConversation(fragment.identifier);
            setTitle(chatBox.getTitle());
            fragment.reloadData();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatman_activity);

        Facebook facebook = Facebook.getInstance();
        Amanuensis clerk = Amanuensis.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);
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
}
