package chat.dim.sechat.group;

import android.content.Intent;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import chat.dim.Entity;
import chat.dim.protocol.ID;
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

        Intent intent = getIntent();
        // get extra info
        ID identifier = Entity.parseID(intent.getStringExtra("ID"));
        ID from = Entity.parseID(intent.getStringExtra("from"));

        if (savedInstanceState == null) {
            fragment = InviteFragment.newInstance(identifier);
            fragment.setFrom(from);
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
