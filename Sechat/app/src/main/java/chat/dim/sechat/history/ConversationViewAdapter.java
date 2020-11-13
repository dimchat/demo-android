package chat.dim.sechat.history;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.protocol.ID;
import chat.dim.protocol.NetworkType;
import chat.dim.sechat.R;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.RecyclerViewAdapter;
import chat.dim.ui.list.RecyclerViewHolder;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ConversationList.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ConversationViewAdapter extends RecyclerViewAdapter<ConversationViewAdapter.ViewHolder, ConversationList> {

    ConversationViewAdapter(ConversationList list, Listener listener) {
        super(list, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.conversations_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConversationList.Item item = dummyList.getItem(position);

        ID identifier = item.getIdentifier();
        if (NetworkType.isGroup(identifier.getType())) {
            holder.userCard.setVisibility(View.GONE);
            holder.groupCard.setVisibility(View.VISIBLE);
            Bitmap logo = item.getLogo();
            holder.logoView.setImageBitmap(logo);
        } else {
            holder.userCard.setVisibility(View.VISIBLE);
            holder.groupCard.setVisibility(View.GONE);
            Bitmap avatar = item.getAvatar();
            holder.avatarView.setImageBitmap(avatar);
        }

        holder.titleView.setText(item.getTitle());
        holder.timeView.setText(item.getTime());
        holder.descView.setText(item.getDesc());

        String badge = item.getUnread();
        if (badge == null) {
            holder.badgeCard.setVisibility(View.GONE);
        } else {
            holder.badgeCard.setVisibility(View.VISIBLE);
            holder.badgeText.setText(badge);
        }

        super.onBindViewHolder(holder, position);
    }

    static class ViewHolder extends RecyclerViewHolder<ConversationList.Item> {

        final CardView groupCard;
        final ImageView logoView;

        final CardView userCard;
        final ImageView avatarView;

        final TextView titleView;
        final TextView timeView;
        final TextView descView;

        final CardView badgeCard;
        final TextView badgeText;

        ViewHolder(View view) {
            super(view);

            groupCard = view.findViewById(R.id.groupCard);
            logoView = view.findViewById(R.id.logoView);

            userCard = view.findViewById(R.id.userCard);
            avatarView = view.findViewById(R.id.avatarView);

            titleView = view.findViewById(R.id.title);
            timeView = view.findViewById(R.id.time);
            descView = view.findViewById(R.id.desc);

            badgeCard = view.findViewById(R.id.badgeCard);
            badgeText = view.findViewById(R.id.badgeText);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + descView.getText() + "'";
        }
    }
}
