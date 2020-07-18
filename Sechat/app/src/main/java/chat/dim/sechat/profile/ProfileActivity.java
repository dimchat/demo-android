package chat.dim.sechat.profile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        Facebook facebook = Facebook.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);
        assert identifier.isValid() : "ID error: " + identifier;
        setTitle(facebook.getNickname(identifier));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ProfileFragment.newInstance(identifier))
                    .commitNow();
        }
    }
}
