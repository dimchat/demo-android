package chat.dim.sechat.search;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.sechat.R;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.ViewAdapter;

/**
 * {@link RecyclerView.Adapter} that can display a {@link DummyContent.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class RecyclerViewAdapter extends ViewAdapter<RecyclerViewAdapter.ViewHolder, DummyContent> {

    RecyclerViewAdapter(DummyContent list, Listener listener) {
        super(list, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.search_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DummyContent.Item item = dummyList.getItem(position);
        holder.mTitleView.setText(item.getTitle());
        holder.mDescView.setText(item.getDesc());

        Bitmap avatar = item.getAvatar();
        holder.mAvatarView.setImageBitmap(avatar);

        super.onBindViewHolder(holder, position);
    }

    static public class ViewHolder extends chat.dim.ui.list.ViewHolder<DummyContent.Item> {

        final ImageView mAvatarView;
        final TextView mTitleView;
        final TextView mDescView;

        ViewHolder(View view) {
            super(view);
            mAvatarView = view.findViewById(R.id.search_avatar);
            mTitleView = view.findViewById(R.id.search_title);
            mDescView = view.findViewById(R.id.search_desc);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mDescView.getText() + "'";
        }
    }
}