package chat.dim.sechat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.network.ServerState;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.sechat.account.AccountFragment;
import chat.dim.sechat.chatbox.ChatboxActivity;
import chat.dim.sechat.contacts.ContactFragment;
import chat.dim.sechat.history.ConversationFragment;
import chat.dim.sechat.push.jpush.JPushManager;
import chat.dim.sechat.register.RegisterActivity;

public class MainActivity extends AppCompatActivity implements Observer {

    private String originTitle = null;
    public String serverState = null;

    public MainActivity() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.ServerStateChanged);
        nc.addObserver(this, NotificationNames.GroupCreated);
        nc.addObserver(this, NotificationNames.StartChat);
    }

    @Override
    public void onDestroy() {
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.ServerStateChanged);
        nc.removeObserver(this, NotificationNames.GroupCreated);
        nc.removeObserver(this, NotificationNames.StartChat);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        switch (name) {
            case NotificationNames.ServerStateChanged: {
                serverState = (String) info.get("state");
                Message msg = new Message();
                msgHandler.sendMessage(msg);
                break;
            }
            case NotificationNames.GroupCreated: {
                ID entity = (ID) info.get("ID");
                if (entity != null) {
                    startChat(entity);
                }
                break;
            }
            case NotificationNames.StartChat: {
                ID entity = (ID) info.get("ID");
                if (entity != null) {
                    startChat(entity);
                }
                break;
            }
        }
    }

    private void startChat(ID entity) {
        Intent intent = new Intent();
        intent.setClass(this, ChatboxActivity.class);
        intent.putExtra("ID", entity.toString());
        startActivity(intent);
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            refreshTitle();
        }
    };

    private void refreshTitle() {
        CharSequence status;
        if (serverState == null) {
            status = null;
        } else if (serverState.equals(ServerState.DEFAULT)) {
            status = getText(R.string.server_default);
        } else if (serverState.equals(ServerState.CONNECTING)) {
            status = getText(R.string.server_connecting);
        } else if (serverState.equals(ServerState.CONNECTED)) {
            status = getText(R.string.server_connected);
        } else if (serverState.equals(ServerState.HANDSHAKING)) {
            status = getText(R.string.server_handshaking);
        } else if (serverState.equals(ServerState.ERROR)) {
            status = getText(R.string.server_error);
        } else if (serverState.equals(ServerState.RUNNING)) {
            status = null;
        } else {
            status = null;
        }
        if (status == null) {
            setTitle(originTitle);
        } else {
            setTitle(originTitle + " (" + status + ")");
        }
    }

    @Override
    public void setTitle(int titleId) {
        CharSequence title = getText(titleId);
        if (title instanceof String) {
            originTitle = (String) title;
        }
        //super.setTitle(titleId);
        refreshTitle();
    }

    private void setDefaultFragment() {
        setFragment(new ConversationFragment());
    }

    private void setFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.content, fragment);
        transaction.commit();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener = item -> {
                switch (item.getItemId()) {
                    case R.id.navigation_history:
                        setFragment(new ConversationFragment());
                        return true;
                    case R.id.navigation_contacts:
                        setFragment(new ContactFragment());
                        return true;
                    case R.id.navigation_more:
                        setFragment(new AccountFragment());
                        return true;
                }
                return false;
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        navView.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        setDefaultFragment();

        SechatApp.launch(getApplication(), this);

        Client client = Client.getInstance();
        User user = client.getCurrentUser();
        if (user == null) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), RegisterActivity.class);
            startActivity(intent);
        } else {
            Meta meta = user.getMeta();
            if (meta == null) {
                throw new NullPointerException("failed to get user meta: " + user);
            }
            Profile profile = user.getProfile();
            // check profile
            Facebook facebook = Facebook.getInstance();
            if (facebook.isSigned(profile)) {
                profile.remove(chat.dim.common.Facebook.EXPIRES_KEY);
                Messenger messenger = Messenger.getInstance();
                messenger.postProfile(profile, meta);
            }
            //将用户地址设为别名
            JPushManager.getInstance().setAlias(user.identifier.address.toString());
        }
    }
}
