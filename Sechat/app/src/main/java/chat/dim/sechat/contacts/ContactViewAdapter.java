package chat.dim.sechat.contacts;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Map;

import chat.dim.ID;
import chat.dim.model.Facebook;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.sechat.R;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.RecyclerViewAdapter;
import chat.dim.ui.list.RecyclerViewHolder;

/**
 * {@link RecyclerView.Adapter} that can display a {@link ContactList.Item} and makes a call to the
 * specified {@link Listener}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ContactViewAdapter extends RecyclerViewAdapter<ContactViewAdapter.ViewHolder, ContactList> {

    public ContactViewAdapter(ContactList list, Listener listener) {
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
        super.onBindViewHolder(holder, position);
        holder.refresh();
    }

    public static class ViewHolder extends RecyclerViewHolder<ContactList.Item> implements Observer {

        final ImageView mAvatarView;
        final TextView mTitleView;
        final TextView mDescView;

        ViewHolder(View view) {
            super(view);
            mAvatarView = view.findViewById(R.id.avatarView);
            mTitleView = view.findViewById(R.id.title);
            mDescView = view.findViewById(R.id.desc);

            NotificationCenter nc = NotificationCenter.getInstance();
            nc.addObserver(this, NotificationNames.ProfileUpdated);
            nc.addObserver(this, NotificationNames.FileDownloadSuccess);
        }

        @Override
        public void finalize() throws Throwable {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.removeObserver(this, NotificationNames.ProfileUpdated);
            nc.removeObserver(this, NotificationNames.FileDownloadSuccess);
            super.finalize();
        }

        @Override
        public void onReceiveNotification(Notification notification) {
            String name = notification.name;
            Map info = notification.userInfo;
            assert name != null && info != null : "notification error: " + notification;
            if (name.equals(NotificationNames.ProfileUpdated)) {
                ID user = (ID) info.get("ID");
                if (user.equals(item.getIdentifier())) {
                    Message msg = new Message();
                    msgHandler.sendMessage(msg);
                }
            } else if (name.equals(NotificationNames.FileDownloadSuccess)) {
                Facebook facebook = Facebook.getInstance();
                String avatar = facebook.getAvatar(item.getIdentifier());
                String path = (String) info.get("path");
                if (avatar != null && avatar.equals(path)) {
                    Message msg = new Message();
                    msgHandler.sendMessage(msg);
                }
            }
        }

        @SuppressLint("HandlerLeak")
        private final Handler msgHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                refresh();
            }
        };
        private void refresh() {
            mTitleView.setText(item.getTitle());
            mDescView.setText(item.getDesc());

            Bitmap avatar = item.getAvatar();
            mAvatarView.setImageBitmap(avatar);
        }

        @NonNull
        @Override
        public String toString() {
            return super.toString() + " '" + mDescView.getText() + "'";
        }
    }
}
