package chat.dim.sechat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Map;

import chat.dim.Meta;
import chat.dim.Profile;
import chat.dim.User;
import chat.dim.model.Messenger;
import chat.dim.network.Server;
import chat.dim.network.StateMachine;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.Observer;
import chat.dim.sechat.account.AccountFragment;
import chat.dim.sechat.contacts.ContactFragment;
import chat.dim.sechat.history.ConversationFragment;
import chat.dim.sechat.register.RegisterActivity;
import chat.dim.sechat.search.SearchActivity;
import chat.dim.ui.Resources;

public class MainActivity extends AppCompatActivity implements Observer {

    private String originTitle = null;

    public MainActivity() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, Server.ServerStateChanged);
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        Map info = notification.userInfo;
        if (Server.ServerStateChanged.equals(notification.name)) {
            String name = (String) info.get("state");
            Message msg = new Message();
            if (name == null) {
                return;
            } else if (name.equals(StateMachine.defaultState)) {
                msg.what = 0;
            } else if (name.equals(StateMachine.connectingState)) {
                msg.what = R.string.server_connecting;
            } else if (name.equals(StateMachine.connectedState)) {
                msg.what = R.string.server_connected;
            } else if (name.equals(StateMachine.handshakingState)) {
                msg.what = R.string.server_handshaking;
            } else if (name.equals(StateMachine.errorState)) {
                msg.what = R.string.server_error;
            } else if (name.equals(StateMachine.stoppedState)) {
                msg.what = R.string.server_stopped;
            } else if (name.equals(StateMachine.runningState)) {
                msg.what = 0;
            }
            msgHandler.sendMessage(msg);
        }
    }

    @SuppressLint("HandlerLeak")
    private final Handler msgHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            showStatus(msg.what);
        }
    };

    private void showStatus(int sid) {
        if (sid == 0) {
            setTitle(originTitle);
        } else {
            CharSequence status = Resources.getText(this, sid);
            setTitle(originTitle + " (" + status + ")");
        }
    }

    @Override
    public void setTitle(int titleId) {
        CharSequence title = Resources.getText(this, titleId);
        if (title instanceof String) {
            originTitle = (String) title;
        }
        super.setTitle(titleId);
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

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
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
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
            if (profile != null && profile.get("data") != null && profile.get("signature") != null) {
                profile.remove(chat.dim.common.Facebook.EXPIRES_KEY);
                Messenger messenger = Messenger.getInstance();
                messenger.postProfile(profile, meta);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.right_top_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.search_user) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        }
        return true;
    }
}
