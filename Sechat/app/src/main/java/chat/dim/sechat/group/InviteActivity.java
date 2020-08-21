package chat.dim.sechat.group;

import android.content.Intent;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;

public class InviteActivity extends AppCompatActivity {

    private InviteFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Facebook facebook = Facebook.getInstance();
        Intent intent = getIntent();
        // get extra info
        ID identifier = facebook.getID(intent.getStringExtra("ID"));
        ID from = facebook.getID(intent.getStringExtra("from"));

        if (savedInstanceState == null) {
            fragment = InviteFragment.newInstance(identifier);
            fragment.from = from;
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, fragment)
                    .commitNow();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.group_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save: {
                fragment.updateMembers();
                break;
            }
            case android.R.id.home: {
                finish();
                break;
            }
        }
        return true;
    }
}
