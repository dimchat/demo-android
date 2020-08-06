package chat.dim.sechat.chatbox;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.database.Database;
import chat.dim.filesys.ExternalStorage;
import chat.dim.http.HTTPClient;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.R;
import chat.dim.sechat.contacts.ContactList;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.ui.image.ImageViewerActivity;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.RecyclerViewAdapter;
import chat.dim.ui.list.RecyclerViewHolder;
import chat.dim.ui.media.AudioPlayer;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ContactList.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class MessageViewAdapter extends RecyclerViewAdapter<MessageViewAdapter.ViewHolder, MessageList> {

    public MessageViewAdapter(MessageList list, Listener listener) {
        super(list, listener);
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
        showMessage(iMsg, holder);

        if (MsgType.RECEIVED == type) {
            holder.avatarView.setOnClickListener(v -> {
                Object sender = iMsg.envelope.sender;
                Intent intent = new Intent();
                intent.setClass(context, ProfileActivity.class);
                intent.putExtra("ID", sender.toString());
                context.startActivity(intent);
            });
        }

        if (holder.frameLayout != null) {
            holder.frameLayout.setOnClickListener(v -> playAudio(iMsg, context));
        }

        if (holder.imgView != null) {
            holder.imgView.setOnClickListener(v -> showImage(iMsg, context));
        }

        super.onBindViewHolder(holder, position);
    }

    private void playAudio(InstantMessage iMsg, Context context) {
        if (iMsg.content instanceof AudioContent) {
            AudioContent content = (AudioContent) iMsg.content;
            String filename = content.getFilename();
            if (filename != null) {
                String path = Database.getCacheFilePath(filename);
                if (Database.exists(path)) {
                    System.out.println("playing " + path);
                    AudioPlayer player = new AudioPlayer((ContextWrapper) context);
                    player.startPlay(Uri.parse(path));
                    System.out.println("go " + path);
                }
            }
        }
    }

    private void showImage(InstantMessage iMsg, Context context) {
        if (iMsg.content instanceof ImageContent) {
            ImageContent content = (ImageContent) iMsg.content;
            showImage(content.getFilename(), UserViewModel.getUsername(iMsg.envelope.sender), context);
        }
    }

    private void showImage(String filename, String sender, Context context) {
        String path = HTTPClient.getCachePath(filename);
        if (!ExternalStorage.exists(path)) {
            return;
        }
        ImageViewerActivity.show(context, Uri.parse(path), sender);
    }

    private Facebook facebook = Facebook.getInstance();
    private ConversationDatabase msgDB = ConversationDatabase.getInstance();

    private void showMessage(InstantMessage iMsg, ViewHolder viewHolder) {
        ID sender = facebook.getID(iMsg.envelope.sender);
        Content content = iMsg.content;

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
            String name = UserViewModel.getNickname(sender);
            viewHolder.nameView.setText(name);
        }

        // show by content type
        if (content instanceof Command) {
            Command cmd = (Command) content;
            String text = msgDB.getCommandText(cmd, sender);
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

    public static class ViewHolder extends RecyclerViewHolder<ContactList.Item> {

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

        ViewHolder(View view) {
            super(view);
            leftLayout = view.findViewById(R.id.recv_msg);
            centerLayout = view.findViewById(R.id.cmd_msg);
            rightLayout = view.findViewById(R.id.sent_msg);
        }
    }
}
