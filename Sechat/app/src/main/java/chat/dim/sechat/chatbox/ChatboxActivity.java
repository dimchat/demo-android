package chat.dim.sechat.chatbox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import chat.dim.common.Amanuensis;
import chat.dim.common.Conversation;
import chat.dim.common.Facebook;
import chat.dim.mkm.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ui.chatbox.ChatboxFragment;

public class ChatboxActivity extends AppCompatActivity {

    public Conversation chatBox = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbox_activity);
        updateTitle();
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, ChatboxFragment.newInstance())
                    .commitNow();
        }
    }

    private void updateTitle() {
        String string = getIntent().getStringExtra("ID");
        Facebook facebook = Facebook.getInstance();
        ID identifier = facebook.getID(string);
        if (identifier == null) {
            return;
        }
        Amanuensis clerk = Amanuensis.getInstance();
        chatBox = clerk.getConversation(identifier);
        setTitle(chatBox.getTitle());
    }
}
