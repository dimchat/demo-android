package chat.dim.sechat.chatbox;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import chat.dim.ID;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;

public class ChatboxActivity extends AppCompatActivity {

    private ID identifier = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbox_activity);

        Facebook facebook = Facebook.getInstance();
        Amanuensis clerk = Amanuensis.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        identifier = facebook.getID(string);
        assert identifier != null : "ID error: " + string;
        Conversation chatBox = clerk.getConversation(identifier);
        setTitle(chatBox.getTitle());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ChatboxFragment.newInstance(chatBox))
                    .commitNow();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chatbox_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.more) {
            Intent intent = new Intent();
            intent.setClass(getApplicationContext(), ChatManageActivity.class);
            intent.putExtra("ID", identifier.toString());
            startActivity(intent);
        }
        return true;
    }
}
