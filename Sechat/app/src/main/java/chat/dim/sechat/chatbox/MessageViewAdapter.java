package chat.dim.sechat.chatbox;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.filesys.Paths;
import chat.dim.http.FileTransfer;
import chat.dim.model.ConversationDatabase;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.R;
import chat.dim.sechat.contacts.ContactList;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.threading.MainThread;
import chat.dim.ui.Alert;
import chat.dim.ui.image.ImageViewerActivity;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.RecyclerViewAdapter;
import chat.dim.ui.list.RecyclerViewHolder;
import chat.dim.ui.media.AudioPlayer;
import chat.dim.utils.Log;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ContactList.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MessageViewAdapter extends RecyclerViewAdapter<MessageViewAdapter.ViewHolder, MessageList> {

    private AudioPlayer audioPlayer = null;

    public MessageViewAdapter(MessageList list, Listener listener) {
        super(list, listener);
    }

    void setAudioPlayer(AudioPlayer player) {
        if (audioPlayer != null) {
            audioPlayer.stopPlay();
        }
        audioPlayer = player;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.chatbox_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageList.Item item = dummyList.getItem(position);

        InstantMessage iMsg = item.msg;
        View view = holder.itemView;
        Context context = view.getContext();

        MsgType type = ChatboxViewModel.getType(iMsg, dummyList.chatBox);
        if (MsgType.SENT == type) {
            holder.leftLayout.setVisibility(View.GONE);
            holder.centerLayout.setVisibility(View.GONE);
            holder.rightLayout.setVisibility(View.VISIBLE);

            holder.timeView = view.findViewById(R.id.time_text);
            holder.avatarView = view.findViewById(R.id.sent_avatar);
            holder.nameView = view.findViewById(R.id.sent_name);
            holder.frameLayout = view.findViewById(R.id.sent_frame);
            holder.speakerView = view.findViewById(R.id.sent_speaker);
            holder.msgView = view.findViewById(R.id.sent_text);
            holder.imgView = view.findViewById(R.id.sent_image);

            holder.failedIndicator = view.findViewById(R.id.failedIndicator);
            holder.sendingIndicator = view.findViewById(R.id.sendingIndicator);
        } else if (MsgType.RECEIVED == type) {
            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.centerLayout.setVisibility(View.GONE);
            holder.rightLayout.setVisibility(View.GONE);

            holder.timeView = view.findViewById(R.id.time_text);
            holder.avatarView = view.findViewById(R.id.recv_avatar);
            holder.nameView = view.findViewById(R.id.recv_name);
            holder.frameLayout = view.findViewById(R.id.recv_frame);
            holder.speakerView = view.findViewById(R.id.recv_speaker);
            holder.msgView = view.findViewById(R.id.recv_text);
            holder.imgView = view.findViewById(R.id.recv_image);
            holder.failureMask = view.findViewById(R.id.fail_mask);
        } else if (MsgType.COMMAND == type) {
            holder.leftLayout.setVisibility(View.GONE);
            holder.centerLayout.setVisibility(View.VISIBLE);
            holder.rightLayout.setVisibility(View.GONE);

            holder.timeView = view.findViewById(R.id.time_text);
            holder.avatarView = null;
            holder.nameView = null;
            holder.frameLayout = null;
            holder.msgView = view.findViewById(R.id.cmd_text);
            holder.imgView = null;
        }
        try {
            showMessage(iMsg, holder);
        } catch (Exception e) {
            e.printStackTrace();
            // FIXME:
            Alert.tips(context, e.getMessage());
        }

        if (MsgType.SENT == type) {
            List<?> traces = (List<?>) iMsg.get("traces");
            if (traces == null || traces.size() == 0) {
                Object error = iMsg.get("error");
                Date time = iMsg.getTime();
                if (error != null || time == null || time.getTime() < (System.currentTimeMillis() - 120 * 1000)) {
                    holder.failedIndicator.setVisibility(View.VISIBLE);
                    holder.sendingIndicator.setVisibility(View.GONE);
                } else {
                    holder.failedIndicator.setVisibility(View.GONE);
                    holder.sendingIndicator.setVisibility(View.VISIBLE);
                }
            } else {
                holder.failedIndicator.setVisibility(View.GONE);
                holder.sendingIndicator.setVisibility(View.GONE);
            }
        }

        if (MsgType.RECEIVED == type) {
            holder.avatarView.setOnClickListener(v -> {
                Object sender = iMsg.getSender();
                Intent intent = new Intent();
                intent.setClass(context, ProfileActivity.class);
                intent.putExtra("ID", sender.toString());
                context.startActivity(intent);
            });
            holder.failureMask.setVisibility(View.GONE);
        }

        if (holder.frameLayout != null) {
            holder.frameLayout.setOnClickListener(v -> playAudio(iMsg));
        }

        if (holder.imgView != null) {
            holder.imgView.setOnClickListener(v -> showImage(iMsg, context));
        }

        super.onBindViewHolder(holder, position);
    }

    private void playAudio(InstantMessage iMsg) {
        if (audioPlayer == null) {
            return;
        }
        if (iMsg.getContent() instanceof AudioContent) {
            AudioContent content = (AudioContent) iMsg.getContent();
            FileTransfer ftp = FileTransfer.getInstance();
            String path = ftp.getFilePath(content);
            if (path != null && Paths.exists(path)) {
                Log.info("playing " + path);
                audioPlayer.startPlay(Uri.parse(path));
            }
        }
    }

    private void showImage(InstantMessage iMsg, Context context) {
        if (iMsg.getContent() instanceof ImageContent) {
            ImageContent content = (ImageContent) iMsg.getContent();
            FileTransfer ftp = FileTransfer.getInstance();
            String path = ftp.getFilePath(content);
            Log.info("showing image: " + path);
            if (path != null && Paths.exists(path)) {
                GlobalVariable shared = GlobalVariable.getInstance();
                SharedFacebook facebook = shared.facebook;
                String sender = facebook.getName(iMsg.getSender());
                ImageViewerActivity.show(context, Uri.parse(path), sender);
            }
        }
    }

    private final ConversationDatabase msgDB = ConversationDatabase.getInstance();

    private void showMessage(InstantMessage iMsg, ViewHolder viewHolder) {

        ID sender = iMsg.getSender();

        // time
        String time = msgDB.getTimeString(iMsg);
        if (time == null) {
            viewHolder.timeView.setVisibility(View.GONE);
        } else {
            viewHolder.timeView.setVisibility(View.VISIBLE);
            viewHolder.timeView.setText(time);
        }

        // avatar
        if (viewHolder.avatarView != null) {
            Bitmap avatar = UserViewModel.getAvatar(sender);
            viewHolder.avatarView.setImageBitmap(avatar);
        }

        // name
        if (viewHolder.nameView != null) {
            GlobalVariable shared = GlobalVariable.getInstance();
            SharedFacebook facebook = shared.facebook;
            String name = facebook.getName(sender);
            viewHolder.nameView.setText(name);
        }

        // show by content type
        Content content = iMsg.getContent();
        if (content instanceof Command) {
            Command command = (Command) content;
            String text = msgDB.getCommandText(command, sender);
            //viewHolder.frameLayout.setVisibility(View.VISIBLE);
            viewHolder.msgView.setVisibility(View.VISIBLE);
            viewHolder.msgView.setText(text);
            //viewHolder.imgView.setVisibility(View.GONE);
        } else if (content instanceof TextContent) {
            showTextMessage((TextContent) content, viewHolder);
        } else if (content instanceof ImageContent) {
            showImageMessage((ImageContent) content, viewHolder);
        } else if (content instanceof AudioContent) {
            showAudioMessage((AudioContent) content, viewHolder);
        } else {
            String text = msgDB.getContentText(content);
            //viewHolder.frameLayout.setVisibility(View.VISIBLE);
            viewHolder.msgView.setVisibility(View.VISIBLE);
            viewHolder.msgView.setText(text);
            //viewHolder.imgView.setVisibility(View.GONE);
        }
    }

    private void showTextMessage(TextContent content, ViewHolder holder) {
        holder.frameLayout.setVisibility(View.VISIBLE);
        holder.speakerView.setVisibility(View.GONE);
        holder.msgView.setVisibility(View.VISIBLE);
        holder.imgView.setVisibility(View.GONE);

        holder.msgView.setText(content.getText());
    }
    private void showImageMessage(ImageContent content, ViewHolder holder) {
        holder.frameLayout.setVisibility(View.GONE);
        holder.speakerView.setVisibility(View.GONE);
        holder.msgView.setVisibility(View.GONE);
        holder.imgView.setVisibility(View.VISIBLE);

        Uri uri = ChatboxViewModel.getImageUri(content);
        if (uri == null) {
            Bitmap bitmap = ChatboxViewModel.getThumbnail(content);
            if (bitmap == null) {
                // should not happen
                uri = ChatboxViewModel.getGalleryUri();
                holder.imgView.setImageURI(uri);
            } else {
                holder.imgView.setImageBitmap(bitmap);
            }
            holder.setFileContent(content);
        } else {
            holder.imgView.setImageURI(uri);
        }
    }
    private void showAudioMessage(AudioContent content, ViewHolder holder) {
        holder.frameLayout.setVisibility(View.VISIBLE);
        holder.speakerView.setVisibility(View.VISIBLE);
        holder.msgView.setVisibility(View.VISIBLE);
        holder.imgView.setVisibility(View.GONE);

        Uri uri = ChatboxViewModel.getAudioUri(content);
        if (uri == null) {
            holder.msgView.setText(R.string.downloading);
            holder.setFileContent(content);
        } else {
            Object duration = content.get("duration");
            if (duration == null) {
                holder.msgView.setText(content.getFilename());
            } else {
                int millis = ((Number) duration).intValue();
                int seconds = Math.round(millis / 1000.0f);
                String msg = String.format(Locale.CHINA, " %d\" ", seconds);
                holder.msgView.setText(msg);
            }
        }
    }

    public static class ViewHolder extends RecyclerViewHolder<ContactList.Item> implements Observer {

        private FileContent fileContent = null;
        private String downloadingURL = null;

        LinearLayout leftLayout;
        LinearLayout rightLayout;
        LinearLayout centerLayout;

        TextView timeView = null;
        ImageView avatarView = null;
        TextView nameView = null;

        LinearLayout frameLayout = null;
        ImageView speakerView = null;
        TextView msgView = null;
        ImageView imgView = null;
        View failureMask = null;

        ImageView failedIndicator = null;
        ProgressBar sendingIndicator = null;

        ViewHolder(View view) {
            super(view);
            leftLayout = view.findViewById(R.id.recv_msg);
            centerLayout = view.findViewById(R.id.cmd_msg);
            rightLayout = view.findViewById(R.id.sent_msg);
        }

        @Override
        protected void finalize() throws Throwable {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.removeObserver(this, NotificationNames.FileDownloadSuccess);
            nc.removeObserver(this, NotificationNames.FileDownloadFailure);
            super.finalize();
        }

        void setFileContent(FileContent content) {
            fileContent = content;
            downloadingURL = content.getURL();
            Log.info("waiting for downloading: " + downloadingURL);
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.addObserver(this, NotificationNames.FileDownloadSuccess);
            nc.addObserver(this, NotificationNames.FileDownloadFailure);
        }

        void onDownloadSuccess() throws MalformedURLException {
            Log.info("success to download: " + downloadingURL);
            Uri uri = ChatboxViewModel.getFileUri(fileContent);
            if (uri == null) {
                throw new NullPointerException("failed to get image URL: " + fileContent);
            }
            if (fileContent instanceof ImageContent) {
                assert imgView != null : "should not happen";
                imgView.setImageURI(uri);
            } else if (fileContent instanceof AudioContent) {
                assert msgView != null : "should not happen";
                Object duration = fileContent.get("duration");
                if (duration == null) {
                    msgView.setText(fileContent.getFilename());
                } else {
                    int millis = ((Number) duration).intValue();
                    int seconds = Math.round(millis / 1000.0f);
                    String msg = String.format(Locale.CHINA, " %d\" ", seconds);
                    msgView.setText(msg);
                }
            }
        }

        void onDownloadFailure() {
            Log.error("failed to download: " + downloadingURL);
            if (fileContent instanceof ImageContent) {
                failureMask.setVisibility(View.VISIBLE);
            } else if (fileContent instanceof AudioContent) {
                msgView.setText(R.string.download_audio_failed);
            }
        }

        @Override
        public void onReceiveNotification(Notification notification) {
            String name = notification.name;
            Map<String, Object> info = notification.userInfo;
            assert name != null && info != null : "notification error: " + notification;
            String url = (String) info.get("URL");
            if (url != null && url.equals(downloadingURL)) {
                if (name.equals(NotificationNames.FileDownloadSuccess)) {
                    MainThread.call(() -> {
                        try {
                            onDownloadSuccess();
                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else if (name.equals(NotificationNames.FileDownloadFailure)) {
                    MainThread.call(this::onDownloadFailure);
                }
            }
        }
    }
}
