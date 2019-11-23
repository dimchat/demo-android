package chat.dim.sechat.chatbox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.mkm.ID;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ui.chatbox.ChatboxFragment;

public class ChatboxActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbox_activity);

        Facebook facebook = Facebook.getInstance();
        Amanuensis clerk = Amanuensis.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);
        assert identifier.isValid();
        Conversation chatBox = clerk.getConversation(identifier);
        setTitle(chatBox.getTitle());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ChatboxFragment.newInstance(chatBox))
                    .commitNow();
        }
    }
}
