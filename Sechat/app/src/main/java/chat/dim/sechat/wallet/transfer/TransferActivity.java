package chat.dim.sechat.wallet.transfer;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;

import chat.dim.Entity;
import chat.dim.model.Facebook;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.wallet.WalletName;

public class TransferActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transfer_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Facebook facebook = Facebook.getInstance();
        // get extra info
        String wallet = getIntent().getStringExtra("wallet");
        String string = getIntent().getStringExtra("ID");
        ID identifier = Entity.parseID(string);

        String title = String.format("%s %s %s", wallet, getText(R.string.transfer), facebook.getUsername(identifier));
        setTitle(title);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, TransferFragment.newInstance(identifier, WalletName.fromString(wallet)))
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
