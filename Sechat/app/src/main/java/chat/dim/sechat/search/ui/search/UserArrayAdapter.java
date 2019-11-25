package chat.dim.sechat.search.ui.search;

import android.content.Context;
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

public class UserArrayAdapter extends ArrayAdapter<ID> {

    private final int resId;

    UserArrayAdapter(Context context, int resource, List<ID> objects) {
        super(context, resource, objects);
        resId = resource;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ID identifier = getItem(position);
        View view;
        ViewHolder viewHolder;

        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resId, null);
            viewHolder = new ViewHolder();
            viewHolder.avatarView = view.findViewById(R.id.search_avatar);
            viewHolder.titleView = view.findViewById(R.id.search_title);
            viewHolder.descView = view.findViewById(R.id.search_desc);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        showUser(identifier, viewHolder);

        return view;
    }

    private Facebook facebook = Facebook.getInstance();

    private void showUser(ID identifier, ViewHolder viewHolder) {
        String name = facebook.getNickname(identifier);
        if (name == null || name.length() == 0) {
            name = identifier.name;
            if (name == null || name.length() == 0) {
                name = identifier.address.toString();
            }
        }
        String number = facebook.getNumberString(identifier);

        viewHolder.titleView.setText(name);
        viewHolder.descView.setText(number);
    }

    class ViewHolder {
        ImageView avatarView;
        TextView titleView;
        TextView descView;
    }
}
