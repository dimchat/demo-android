package chat.dim.sechat.profile;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Facebook facebook = Facebook.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);
        assert identifier.isValid() : "ID error: " + identifier;
        setTitle(facebook.getUsername(identifier));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ProfileFragment.newInstance(identifier))
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
