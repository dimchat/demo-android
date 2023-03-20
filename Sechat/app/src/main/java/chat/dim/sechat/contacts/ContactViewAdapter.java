package chat.dim.sechat.contacts;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URL;
import java.util.Map;

import chat.dim.GlobalVariable;
import chat.dim.SharedFacebook;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.threading.MainThread;
import chat.dim.type.Pair;
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
            nc.addObserver(this, NotificationNames.DocumentUpdated);
            nc.addObserver(this, NotificationNames.FileDownloadSuccess);
        }

        @Override
        public void finalize() throws Throwable {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.removeObserver(this, NotificationNames.DocumentUpdated);
            nc.removeObserver(this, NotificationNames.FileDownloadSuccess);
            super.finalize();
        }

        @Override
        public void onReceiveNotification(Notification notification) {
            String name = notification.name;
            Map info = notification.userInfo;
            assert name != null && info != null : "notification error: " + notification;
            if (name.equals(NotificationNames.DocumentUpdated)) {
                ID user = ID.parse(info.get("ID"));
                if (user.equals(item.getIdentifier())) {
                    MainThread.call(this::refresh);
                }
            } else if (name.equals(NotificationNames.FileDownloadSuccess)) {
                GlobalVariable shared = GlobalVariable.getInstance();
                SharedFacebook facebook = shared.facebook;
                Pair<String, URL> avatars = facebook.getAvatar(item.getIdentifier());
                String path = (String) info.get("path");
                if (avatars.first != null && avatars.first.equals(path)) {
                    MainThread.call(this::refresh);
                }
            }
        }

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
