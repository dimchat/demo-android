package chat.dim.sechat.chatbox;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.filesys.ExternalStorage;
import chat.dim.http.HTTPClient;
import chat.dim.model.Conversation;
import chat.dim.model.ConversationDatabase;
import chat.dim.model.Facebook;
import chat.dim.protocol.Command;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.R;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.ui.image.ImageViewerActivity;

public class MessageArrayAdapter extends ArrayAdapter<InstantMessage> {

    private final int resId;

    Conversation chatBox = null;

    MessageArrayAdapter(Context context, int resource, List<InstantMessage> objects) {
        super(context, resource, objects);
        resId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;
        InstantMessage iMsg = getItem(position);
        assert iMsg != null : "failed to get message with position: " + position;

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resId, null);
            viewHolder = new ViewHolder();
            viewHolder.leftLayout = view.findViewById(R.id.recv_msg);
            viewHolder.centerLayout = view.findViewById(R.id.cmd_msg);
            viewHolder.rightLayout = view.findViewById(R.id.sent_msg);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        MsgType type = ChatboxViewModel.getType(iMsg, chatBox);
        if (MsgType.SENT == type) {
            viewHolder.leftLayout.setVisibility(View.GONE);
            viewHolder.centerLayout.setVisibility(View.GONE);
            viewHolder.rightLayout.setVisibility(View.VISIBLE);
            viewHolder.timeView = view.findViewById(R.id.time_text);
            viewHolder.avatarView = view.findViewById(R.id.sent_avatar);
            viewHolder.nameView = view.findViewById(R.id.sent_name);
            viewHolder.frameLayout = view.findViewById(R.id.sent_frame);
            viewHolder.msgView = view.findViewById(R.id.sent_text);
            viewHolder.imgView = view.findViewById(R.id.sent_image);
        } else if (MsgType.RECEIVED == type) {
            viewHolder.leftLayout.setVisibility(View.VISIBLE);
            viewHolder.centerLayout.setVisibility(View.GONE);
            viewHolder.rightLayout.setVisibility(View.GONE);
            viewHolder.timeView = view.findViewById(R.id.time_text);
            viewHolder.avatarView = view.findViewById(R.id.recv_avatar);
            viewHolder.nameView = view.findViewById(R.id.recv_name);
            viewHolder.frameLayout = view.findViewById(R.id.recv_frame);
            viewHolder.msgView = view.findViewById(R.id.recv_text);
            viewHolder.imgView = view.findViewById(R.id.recv_image);
        } else if (MsgType.COMMAND == type) {
            viewHolder.leftLayout.setVisibility(View.GONE);
            viewHolder.centerLayout.setVisibility(View.VISIBLE);
            viewHolder.rightLayout.setVisibility(View.GONE);
            viewHolder.timeView = view.findViewById(R.id.time_text);
            viewHolder.avatarView = null;
            viewHolder.nameView = null;
            viewHolder.frameLayout = null;
            viewHolder.msgView = view.findViewById(R.id.cmd_text);
            viewHolder.imgView = null;
        }
        showMessage(iMsg, viewHolder);

        if (MsgType.RECEIVED == type) {
            viewHolder.avatarView.setOnClickListener(v -> {
                Object sender = iMsg.envelope.sender;
                Intent intent = new Intent();
                intent.setClass(getContext(), ProfileActivity.class);
                intent.putExtra("ID", sender.toString());
                getContext().startActivity(intent);
            });
        }

        if (viewHolder.imgView != null) {
            viewHolder.imgView.setOnClickListener(v -> showImage(iMsg));
        }

        return view;
    }

    private void showImage(InstantMessage iMsg) {
        if (iMsg.content instanceof ImageContent) {
            ImageContent content = (ImageContent) iMsg.content;
            showImage(content.getFilename(), UserViewModel.getUsername(iMsg.envelope.sender));
        }
    }

    private void showImage(String filename, String sender) {
        String path = HTTPClient.getCachePath(filename);
        if (!ExternalStorage.exists(path)) {
            return;
        }
        ImageViewerActivity.show(getContext(), Uri.parse(path), sender);
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
            viewHolder.imgView.setImageURI(ChatboxViewModel.getImageUri(imageContent, iMsg));
        } else if (content instanceof Command) {
            Command cmd = (Command) content;
            String text = msgDB.getCommandText(cmd, sender);
            //viewHolder.frameLayout.setVisibility(View.VISIBLE);
            viewHolder.msgView.setVisibility(View.VISIBLE);
            viewHolder.msgView.setText(text);
            //viewHolder.imgView.setVisibility(View.GONE);
        }
    }

    static class ViewHolder {
        LinearLayout leftLayout = null;
        LinearLayout rightLayout = null;
        LinearLayout centerLayout = null;

        TextView timeView = null;
        ImageView avatarView = null;
        TextView nameView = null;
        FrameLayout frameLayout = null;
        TextView msgView = null;
        ImageView imgView = null;
    }
}
