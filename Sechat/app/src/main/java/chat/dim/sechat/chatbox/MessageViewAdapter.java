package chat.dim.sechat.chatbox;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.filesys.ExternalStorage;
import chat.dim.http.HTTPClient;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
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

        if (holder.imgView != null) {
            holder.imgView.setOnClickListener(v -> showImage(iMsg, context));
        }

        super.onBindViewHolder(holder, position);
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

        // message
        if (content instanceof TextContent) {
            TextContent textContent = (TextContent) content;
            viewHolder.frameLayout.setVisibility(View.VISIBLE);
            viewHolder.msgView.setVisibility(View.VISIBLE);
            viewHolder.msgView.setText(textContent.getText());

            viewHolder.imgView.setVisibility(View.GONE);
        } else if (content instanceof ImageContent) {
            ImageContent imageContent = (ImageContent) content;
            viewHolder.frameLayout.setVisibility(View.GONE);
            viewHolder.msgView.setVisibility(View.GONE);

            viewHolder.imgView.setVisibility(View.VISIBLE);
            Uri uri = ChatboxViewModel.getImageUri(imageContent, iMsg);
            if (uri == null) {
                Bitmap bitmap = ChatboxViewModel.getThumbnail(imageContent);
                if (bitmap == null) {
                    // should not happen
                    uri = ChatboxViewModel.getGalleryUri();
                    viewHolder.imgView.setImageURI(uri);
                } else {
                    viewHolder.imgView.setImageBitmap(bitmap);
                }
            } else {
                viewHolder.imgView.setImageURI(uri);
            }
        } else if (content instanceof Command) {
            Command cmd = (Command) content;
            String text = msgDB.getCommandText(cmd, sender);
            //viewHolder.frameLayout.setVisibility(View.VISIBLE);
            viewHolder.msgView.setVisibility(View.VISIBLE);
            viewHolder.msgView.setText(text);
            //viewHolder.imgView.setVisibility(View.GONE);
        }
    }

    public static class ViewHolder extends RecyclerViewHolder<ContactList.Item> {

        LinearLayout leftLayout;
        LinearLayout rightLayout;
        LinearLayout centerLayout;

        TextView timeView = null;
        ImageView avatarView = null;
        TextView nameView = null;

        FrameLayout frameLayout = null;
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
