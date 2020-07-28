package chat.dim.sechat.history;

import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.ID;
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
                .inflate(R.layout.fragment_conversation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DummyContent.Item item = dummyList.getItem(position);

        ID identifier = item.getIdentifier();
        if (identifier.isGroup()) {
            holder.userCard.setVisibility(View.GONE);
            holder.groupCard.setVisibility(View.VISIBLE);
            Uri logo = item.getLogoUri();
            holder.logoView.setImageURI(logo);
        } else {
            holder.userCard.setVisibility(View.VISIBLE);
            holder.groupCard.setVisibility(View.GONE);
            Uri avatar = item.getAvatarUri();
            holder.avatarView.setImageURI(avatar);
        }

        holder.titleView.setText(item.getTitle());
        holder.descView.setText(item.getDesc());

        super.onBindViewHolder(holder, position);
    }

    static class ViewHolder extends chat.dim.ui.list.ViewHolder<DummyContent.Item> {

        final CardView groupCard;
        final ImageView logoView;

        final CardView userCard;
        final ImageView avatarView;

        final TextView titleView;
        final TextView descView;

        ViewHolder(View view) {
            super(view);

            groupCard = view.findViewById(R.id.groupCard);
            logoView = view.findViewById(R.id.logoView);

            userCard = view.findViewById(R.id.userCard);
            avatarView = view.findViewById(R.id.avatarView);

            titleView = view.findViewById(R.id.title);
            descView = view.findViewById(R.id.desc);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + descView.getText() + "'";
        }
    }
}
