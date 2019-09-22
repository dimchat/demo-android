package chat.dim.sechat.chatbox.ui.chatbox;

import android.arch.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.List;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.client.Facebook;
import chat.dim.database.ConversationDatabase;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.User;
import chat.dim.protocol.Command;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.Client;

enum MsgType {

    SENT    (1),
    RECEIVED(2),
    COMMAND (3);

    public final int value;

    MsgType(int value) {
        this.value = value;
    }
}

public class ChatboxViewModel extends ViewModel {

    static Client client = Client.getInstance();
    static Facebook facebook = Facebook.getInstance();
    static ConversationDatabase msgDB = ConversationDatabase.getInstance();

    public List<InstantMessage> getMessages(Conversation chatBox) {
        return msgDB.messagesInConversation(chatBox);
    }

    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        return msgDB.insertMessage(iMsg, chatBox);
    }

    public static MsgType getType(InstantMessage iMsg) {
        Content content = iMsg.content;
        if (content instanceof Command) {
            return MsgType.COMMAND;
        }

        ID sender = facebook.getID(iMsg.envelope.sender);

        LocalUser user = client.getCurrentUser();
        if (sender.equals(user.identifier)) {
            return MsgType.SENT;
        } else {
            return MsgType.RECEIVED;
        }
    }
}
