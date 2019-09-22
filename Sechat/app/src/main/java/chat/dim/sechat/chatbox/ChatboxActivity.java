package chat.dim.sechat.chatbox;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.List;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.client.Facebook;
import chat.dim.mkm.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ui.chatbox.ChatboxFragment;
import chat.dim.sechat.chatbox.ui.chatbox.ChatboxViewModel;

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
