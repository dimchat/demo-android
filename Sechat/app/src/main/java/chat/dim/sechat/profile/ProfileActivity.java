package chat.dim.sechat.profile;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.common.Facebook;
import chat.dim.mkm.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.profile.ui.profile.ProfileFragment;

public class ProfileActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile_activity);

        Facebook facebook = Facebook.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);
        assert identifier.isValid();
        setTitle(facebook.getNickname(identifier));

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ProfileFragment.newInstance(identifier))
                    .commitNow();
        }
    }
}
