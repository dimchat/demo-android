package chat.dim.sechat.account;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.sechat.R;

public class UpdateAccountActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.update_account_activity);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, UpdateAccountFragment.newInstance())
                    .commitNow();
        }
    }
}
