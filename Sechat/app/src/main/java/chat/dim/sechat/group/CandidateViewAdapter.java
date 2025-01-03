package chat.dim.sechat.group;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.group.SharedGroupManager;
import chat.dim.mkm.User;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.RecyclerViewAdapter;
import chat.dim.ui.list.RecyclerViewHolder;

/**
 * {@link RecyclerView.Adapter} that can display a {@link CandidateList.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class CandidateViewAdapter extends RecyclerViewAdapter<CandidateViewAdapter.ViewHolder, CandidateList> {

    public ID group = null;
    public ID from = null;

    @SuppressWarnings("unchecked")
    public CandidateViewAdapter(CandidateList list, Listener listener) {
        super(list, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.participants_list_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CandidateList.Item item = dummyList.getItem(position);

        Bitmap avatar = item.getAvatar();
        holder.avatarView.setImageBitmap(avatar);

        holder.titleView.setText(item.getTitle());

        if (from != null && from.equals(item.getIdentifier())) {
            holder.checkBox.setChecked(true);
        } else {
            holder.checkBox.setChecked(false);
        }

        ID identifier = item.getIdentifier();
        if (isForbidden(identifier)) {
            holder.checkBox.setEnabled(false);
        }

        super.onBindViewHolder(holder, position);
    }

    private boolean isForbidden(ID identifier) {
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        SharedGroupManager manager = SharedGroupManager.getInstance();
        if (manager.isOwner(identifier, group)) {
            return true;
        }
        User user = facebook.getCurrentUser();
        assert user != null : "failed to get current user";
        return user.getIdentifier().equals(identifier);
    }

    public static class ViewHolder extends RecyclerViewHolder<CandidateList.Item> {

        final ImageView avatarView;
        final TextView titleView;
        public final CheckBox checkBox;

        ViewHolder(View view) {
            super(view);
            avatarView = view.findViewById(R.id.avatarView);
            titleView = view.findViewById(R.id.title);
            checkBox = view.findViewById(R.id.checkBox);
        }
    }
}
