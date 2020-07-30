package chat.dim.sechat.group;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;

public class InviteActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.invite_activity);

        Facebook facebook = Facebook.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, InviteFragment.newInstance(identifier))
                    .commitNow();
        }
    }
}
