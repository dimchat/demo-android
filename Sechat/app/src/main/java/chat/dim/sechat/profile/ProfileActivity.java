package chat.dim.sechat.profile;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.protocol.ID;
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

        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = ID.parse(string);
        setTitle(facebook.getName(identifier));

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
