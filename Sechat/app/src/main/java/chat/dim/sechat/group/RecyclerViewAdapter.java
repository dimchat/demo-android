package chat.dim.sechat.group;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.sechat.R;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.ViewAdapter;

/**
 * {@link RecyclerView.Adapter} that can display a {@link CandidateList.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class RecyclerViewAdapter extends ViewAdapter<RecyclerViewAdapter.ViewHolder, CandidateList> {

    public RecyclerViewAdapter(CandidateList list, Listener listener) {
        super(list, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.members_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CandidateList.Item item = dummyList.getItem(position);

        Bitmap avatar = item.getAvatar();
        holder.avatarView.setImageBitmap(avatar);

        holder.titleView.setText(item.getTitle());

        holder.checkBox.setChecked(false);

        super.onBindViewHolder(holder, position);
    }

    public static class ViewHolder extends chat.dim.ui.list.ViewHolder<CandidateList.Item> {

        final ImageView avatarView;
        final TextView titleView;
        final CheckBox checkBox;

        ViewHolder(View view) {
            super(view);
            avatarView = view.findViewById(R.id.avatarView);
            titleView = view.findViewById(R.id.title);
            checkBox = view.findViewById(R.id.checkBox);
        }
    }
}
