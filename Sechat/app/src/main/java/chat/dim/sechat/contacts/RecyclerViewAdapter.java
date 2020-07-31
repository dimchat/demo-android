package chat.dim.sechat.contacts;

import android.graphics.Bitmap;
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
 * {@link RecyclerView.Adapter} that can display a {@link ContactList.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class RecyclerViewAdapter extends ViewAdapter<RecyclerViewAdapter.ViewHolder, ContactList> {

    public RecyclerViewAdapter(ContactList list, Listener listener) {
        super(list, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.contacts_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ContactList.Item item = dummyList.getItem(position);
        holder.mTitleView.setText(item.getTitle());
        holder.mDescView.setText(item.getDesc());

        Bitmap avatar = item.getAvatar();
        holder.mAvatarView.setImageBitmap(avatar);

        super.onBindViewHolder(holder, position);
    }

    public static class ViewHolder extends chat.dim.ui.list.ViewHolder<ContactList.Item> {

        final ImageView mAvatarView;
        final TextView mTitleView;
        final TextView mDescView;

        ViewHolder(View view) {
            super(view);
            mAvatarView = view.findViewById(R.id.avatarView);
            mTitleView = view.findViewById(R.id.title);
            mDescView = view.findViewById(R.id.desc);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mDescView.getText() + "'";
        }
    }
}
