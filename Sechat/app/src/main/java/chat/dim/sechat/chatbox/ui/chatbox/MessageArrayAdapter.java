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

import chat.dim.client.Facebook;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
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
            viewHolder.rightLayout = (LinearLayout) view.findViewById(R.id.sent_msg);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        MsgType type = ChatboxViewModel.getType(iMsg);
        if (MsgType.SENT == type) {
            viewHolder.leftLayout.setVisibility(View.GONE);
            viewHolder.rightLayout.setVisibility(View.VISIBLE);
            viewHolder.avatarView = view.findViewById(R.id.right_avatar);
            viewHolder.nameView = view.findViewById(R.id.right_name);
            viewHolder.msgView = view.findViewById(R.id.right_message);
        } else if (MsgType.RECEIVED == type) {
            viewHolder.leftLayout.setVisibility(View.VISIBLE);
            viewHolder.rightLayout.setVisibility(View.GONE);
            viewHolder.avatarView = view.findViewById(R.id.left_avatar);
            viewHolder.nameView = view.findViewById(R.id.left_name);
            viewHolder.msgView = view.findViewById(R.id.left_message);
        }
        showMessage(iMsg, viewHolder);

        return view;
    }

    private void showMessage(InstantMessage iMsg, ViewHolder viewHolder) {
        Facebook facebook = Facebook.getInstance();
        ID sender = facebook.getID(iMsg.envelope.sender);

        // avatar

        // name
        String name = facebook.getNickname(sender);
        viewHolder.nameView.setText(name);

        // message
        if (iMsg.content instanceof TextContent) {
            TextContent content = (TextContent) iMsg.content;
            viewHolder.msgView.setText(content.getText());
        }
    }

    class ViewHolder {
        LinearLayout leftLayout;
        LinearLayout rightLayout;

        ImageView avatarView;
        TextView nameView;
        TextView msgView;
    }
}
