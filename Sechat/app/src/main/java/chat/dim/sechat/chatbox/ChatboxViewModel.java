package chat.dim.sechat.chatbox;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.util.ArrayList;
import java.util.List;

import chat.dim.http.FileTransfer;
import chat.dim.io.Resources;
import chat.dim.mkm.User;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Command;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.file.AudioContent;
import chat.dim.protocol.file.ImageContent;
import chat.dim.sechat.SechatApp;
import chat.dim.sechat.model.EntityViewModel;
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

public class ChatboxViewModel extends EntityViewModel {

    List<InstantMessage> getAllMessages(Conversation chatBox) {
        ConversationDatabase msgDB = ConversationDatabase.getInstance();
        if (msgDB.clearUnreadMessages(chatBox)) {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.postNotification(NotificationNames.HistoryUpdated, this, null);
        }

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

        ID sender = iMsg.getSender();
        if (sender.equals(chatBox.identifier)) {
            return MsgType.RECEIVED;
        }

        List<User> users = getFacebook().getLocalUsers();
        for (User user : users) {
            if (user.getIdentifier().equals(sender)) {
                return MsgType.SENT;
            }
        }
        return MsgType.RECEIVED;
    }

    static Uri getFileUri(FileContent content) {
        FileTransfer ftp = FileTransfer.getInstance();
        String path = ftp.getFilePath(content);
        if (path == null) {
            return null;
        }
        return Uri.parse(path);
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
