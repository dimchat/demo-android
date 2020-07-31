package chat.dim.sechat.chatbox;

import android.arch.lifecycle.ViewModel;
import android.net.Uri;

import java.util.List;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.User;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.network.FtpServer;
import chat.dim.protocol.Command;
import chat.dim.protocol.ImageContent;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.Resources;

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

    List<InstantMessage> getMessages(Conversation chatBox) {
        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        return msgDB.messagesInConversation(chatBox);
    }

    static MsgType getType(InstantMessage iMsg, Conversation chatBox) {
        Content content = iMsg.content;
        if (content instanceof Command) {
            return MsgType.COMMAND;
        }

        Facebook facebook = Facebook.getInstance();
        ID sender = facebook.getID(iMsg.envelope.sender);
        if (sender.equals(chatBox.identifier)) {
            return MsgType.RECEIVED;
        }

        List<User> users = facebook.getLocalUsers();
        for (User user : users) {
            if (user.identifier.equals(sender)) {
                return MsgType.SENT;
            }
        }
        return MsgType.RECEIVED;
    }

    static Uri getImageUri(ImageContent content, InstantMessage iMsg) {
        FtpServer ftp = FtpServer.getInstance();
        String path = ftp.getFilePath(content);
        if (path != null) {
            return Uri.parse(path);
        }
        return Resources.getUriFromMipmap(SechatApp.getInstance().getResources(), android.R.drawable.ic_menu_gallery);
    }
}
