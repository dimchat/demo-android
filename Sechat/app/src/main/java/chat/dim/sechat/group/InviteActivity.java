package chat.dim.sechat.group;

import android.support.v7.app.AppCompatActivity;
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

        Facebook facebook = Facebook.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);

        if (savedInstanceState == null) {
            fragment = InviteFragment.newInstance(identifier);
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
