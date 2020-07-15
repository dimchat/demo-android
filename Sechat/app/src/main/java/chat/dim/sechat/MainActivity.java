package chat.dim.sechat;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import chat.dim.User;
import chat.dim.model.Messenger;
import chat.dim.sechat.account.AccountFragment;
import chat.dim.sechat.contacts.ContactFragment;
import chat.dim.sechat.conversations.ConversationFragment;
import chat.dim.sechat.register.RegisterActivity;
import chat.dim.sechat.search.SearchActivity;

public class MainActivity extends AppCompatActivity {

    private FragmentTransaction transaction;
    private FragmentManager fragmentManager;

    private void setDefaultFragment() {
        setFragment(new ConversationFragment());
    }

    private void setFragment(Fragment fragment) {
        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
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
            Messenger messenger = Messenger.getInstance();
            messenger.postProfile(user.getProfile(), user.getMeta());
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
