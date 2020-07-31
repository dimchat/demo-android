package chat.dim.sechat.group;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;

public class ExpelActivity extends AppCompatActivity {

    private ExpelFragment fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expel_activity);

        Facebook facebook = Facebook.getInstance();
        Intent intent = getIntent();
        // get extra info
        ID identifier = facebook.getID(intent.getStringExtra("ID"));

        if (savedInstanceState == null) {
            fragment = ExpelFragment.newInstance(identifier);
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
        if (item.getItemId() == R.id.save) {
            fragment.updateMembers();
        }
        return true;
    }
}