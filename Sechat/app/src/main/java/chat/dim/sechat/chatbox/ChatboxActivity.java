package chat.dim.sechat.chatbox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ui.chatbox.ChatboxFragment;

public class ChatboxActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbox_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ChatboxFragment.newInstance())
                    .commitNow();
        }
    }
}
