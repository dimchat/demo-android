package chat.dim.sechat;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.network.StateMachine;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.sechat.account.AccountFragment;
import chat.dim.sechat.contacts.ContactFragment;
import chat.dim.sechat.history.ConversationFragment;
import chat.dim.sechat.register.RegisterActivity;
import chat.dim.sechat.search.SearchActivity;

public class MainActivity extends AppCompatActivity implements Observer {

    private String originTitle = null;
    private String serverState = null;

    public MainActivity() {
        super();
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.ServerStateChanged);
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        if (name.equals(NotificationNames.ServerStateChanged)) {
            serverState = (String) info.get("state");
            Message msg = new Message();
            msgHandler.sendMessage(msg);
        }
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
        } else if (serverState.equals(StateMachine.defaultState)) {
            status = getText(R.string.server_default);;
        } else if (serverState.equals(StateMachine.connectingState)) {
            status = getText(R.string.server_connecting);
        } else if (serverState.equals(StateMachine.connectedState)) {
            status = getText(R.string.server_connected);
        } else if (serverState.equals(StateMachine.handshakingState)) {
            status = getText(R.string.server_handshaking);
        } else if (serverState.equals(StateMachine.errorState)) {
            status = getText(R.string.server_error);
        } else if (serverState.equals(StateMachine.stoppedState)) {
            status = getText(R.string.server_stopped);
        } else if (serverState.equals(StateMachine.runningState)) {
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
