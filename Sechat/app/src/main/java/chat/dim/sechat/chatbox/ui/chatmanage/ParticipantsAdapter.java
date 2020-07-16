package chat.dim.sechat.chatbox.ui.chatmanage;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;

public class ParticipantsAdapter extends ArrayAdapter<ID> {

    private final int resId;

    public ParticipantsAdapter(Context context, int resource, List<ID> objects) {
        super(context, resource, objects);
        resId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;
        ID identifier = getItem(position);
        assert identifier != null : "failed to get participant ID with position: " + position;

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resId, null);
            viewHolder = new ViewHolder();
            viewHolder.avatarView = view.findViewById(R.id.avatarView);
            viewHolder.nameView = view.findViewById(R.id.nameView);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        showParticipant(identifier, viewHolder);

        return view;
    }

    private Facebook facebook = Facebook.getInstance();

    private void showParticipant(ID identifier, ViewHolder viewHolder) {
        // avatar
        if (viewHolder.avatarView != null) {
            Uri avatar;
            String url = facebook.getAvatar(identifier);
            if (url == null) {
                avatar = SechatApp.getInstance().getUriFromMipmap(R.mipmap.ic_launcher);
            } else {
                avatar = Uri.parse(url);
            }
            viewHolder.avatarView.setImageURI(avatar);
        }

        // name
        if (viewHolder.nameView != null) {
            String name = facebook.getNickname(identifier);
            viewHolder.nameView.setText(name);
        }
    }

    class ViewHolder {
        ImageView avatarView = null;
        TextView nameView = null;
    }
}
