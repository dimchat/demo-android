package chat.dim.sechat.chatbox.ui.chatbox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import chat.dim.common.Facebook;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.protocol.Command;
import chat.dim.protocol.TextContent;
import chat.dim.sechat.R;

public class MessageArrayAdapter extends ArrayAdapter<InstantMessage> {

    private final int resId;

    MessageArrayAdapter(Context context, int resource, List<InstantMessage> objects) {
        super(context, resource, objects);
        resId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        InstantMessage iMsg = getItem(position);
        View view;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resId, null);
            viewHolder = new ViewHolder();
            viewHolder.leftLayout = (LinearLayout) view.findViewById(R.id.received_msg);
            viewHolder.centerLayout = (LinearLayout) view.findViewById(R.id.cmd_msg);
            viewHolder.rightLayout = (LinearLayout) view.findViewById(R.id.sent_msg);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        MsgType type = ChatboxViewModel.getType(iMsg);
        if (MsgType.SENT == type) {
            viewHolder.leftLayout.setVisibility(View.GONE);
            viewHolder.centerLayout.setVisibility(View.GONE);
            viewHolder.rightLayout.setVisibility(View.VISIBLE);
            viewHolder.avatarView = view.findViewById(R.id.right_avatar);
            viewHolder.nameView = view.findViewById(R.id.right_name);
            viewHolder.msgView = view.findViewById(R.id.right_message);
        } else if (MsgType.RECEIVED == type) {
            viewHolder.leftLayout.setVisibility(View.VISIBLE);
            viewHolder.centerLayout.setVisibility(View.GONE);
            viewHolder.rightLayout.setVisibility(View.GONE);
            viewHolder.avatarView = view.findViewById(R.id.left_avatar);
            viewHolder.nameView = view.findViewById(R.id.left_name);
            viewHolder.msgView = view.findViewById(R.id.left_message);
        } else if (MsgType.COMMAND == type) {
            viewHolder.leftLayout.setVisibility(View.GONE);
            viewHolder.centerLayout.setVisibility(View.VISIBLE);
            viewHolder.rightLayout.setVisibility(View.GONE);
            viewHolder.avatarView = null;
            viewHolder.nameView = null;
            viewHolder.msgView = view.findViewById(R.id.center_message);
        }
        showMessage(iMsg, viewHolder);

        return view;
    }

    private void showMessage(InstantMessage iMsg, ViewHolder viewHolder) {
        Facebook facebook = Facebook.getInstance();
        ID sender = facebook.getID(iMsg.envelope.sender);
        Content content = iMsg.content;

        // avatar

        // name
        if (viewHolder.nameView != null) {
            String name = facebook.getNickname(sender);
            viewHolder.nameView.setText(name);
        }

        // message
        if (content instanceof TextContent) {
            TextContent textContent = (TextContent) content;
            viewHolder.msgView.setText(textContent.getText());
        } else if (content instanceof Command) {
            Command cmd = (Command) content;
            Object text = cmd.get("text");
            if (text == null) {
                text = cmd.get("message");
                if (text == null) {
                    text = cmd.command;
                }
            }
            viewHolder.msgView.setText((String) text);
        }
    }

    class ViewHolder {
        LinearLayout leftLayout = null;
        LinearLayout rightLayout = null;
        LinearLayout centerLayout = null;

        ImageView avatarView = null;
        TextView nameView = null;
        TextView msgView = null;
    }
}
