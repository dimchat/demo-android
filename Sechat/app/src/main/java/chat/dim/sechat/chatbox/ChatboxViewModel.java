package chat.dim.sechat.chatbox;

import android.arch.lifecycle.ViewModel;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.User;
import chat.dim.io.Resources;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.network.FtpServer;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ImageContent;
import chat.dim.sechat.SechatApp;
import chat.dim.ui.image.Images;

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

    List<InstantMessage> getAllMessages(Conversation chatBox) {
        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        msgDB.clearUnreadMessages(chatBox);
        List<InstantMessage> messages = new ArrayList<>();
        InstantMessage iMsg;
        int count = msgDB.numberOfMessages(chatBox);
        for (int index = 0; index < count; ++index) {
            iMsg = msgDB.messageAtIndex(index, chatBox);
            if (iMsg == null) {
                continue;
            }
            messages.add(iMsg);
        }
        return messages;
    }

    static MsgType getType(InstantMessage iMsg, Conversation chatBox) {
        if (iMsg.getContent() instanceof Command) {
            return MsgType.COMMAND;
        }

        ID sender = (ID) iMsg.envelope.getSender();
        if (sender.equals(chatBox.identifier)) {
            return MsgType.RECEIVED;
        }

        List<User> users = Facebook.getInstance().getLocalUsers();
        for (User user : users) {
            if (user.identifier.equals(sender)) {
                return MsgType.SENT;
            }
        }
        return MsgType.RECEIVED;
    }

    private static Uri getFileUri(FileContent content) {
        FtpServer ftp = FtpServer.getInstance();
        String path = null;
        try {
            path = ftp.getFilePath(content);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (path != null) {
            return Uri.parse(path);
        }
        return null;
    }

    static Uri getImageUri(ImageContent content) {
        return getFileUri(content);
    }

    static Bitmap getThumbnail(ImageContent content) {
        byte[] thumbnail = content.getThumbnail();
        if (thumbnail == null) {
            return null;
        }
        // TODO: save thumbnail data into local storage and remove from message content

        Bitmap image = BitmapFactory.decodeByteArray(thumbnail, 0, thumbnail.length);
        Images.Size size = Images.getSize(image);
        size = new Images.Size(size.width << 1, size.height << 1);
        return Images.scale(image, size);
    }

    static Uri getGalleryUri() {
        return gallery;
    }

    static Uri getAudioUri(AudioContent content) {
        return getFileUri(content);
    }

    private static final Uri gallery = Resources.getUriFromMipmap(SechatApp.getInstance().getResources(), android.R.drawable.ic_menu_gallery);
}
