package chat.dim.sechat.chatbox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.ID;
import chat.dim.model.Amanuensis;
import chat.dim.model.Conversation;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ui.chatbox.ChatboxFragment;
import chat.dim.sechat.chatbox.ui.chatmanage.ChatManageFragment;

public class ChatManageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_manage_activity);

        Facebook facebook = Facebook.getInstance();
        Amanuensis clerk = Amanuensis.getInstance();
        // get extra info
        String string = getIntent().getStringExtra("ID");
        ID identifier = facebook.getID(string);
        assert identifier != null : "ID error: " + string;
        Conversation chatBox = clerk.getConversation(identifier);
        setTitle(chatBox.getTitle());

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ChatboxFragment.newInstance(chatBox))
                    .commitNow();
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ChatManageFragment.newInstance(identifier))
                    .commitNow();
        }
    }
}
