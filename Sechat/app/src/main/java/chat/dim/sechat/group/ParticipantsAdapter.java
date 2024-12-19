package chat.dim.sechat.group;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import androidx.cardview.widget.CardView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.net.URL;
import java.util.List;
import java.util.Map;

import chat.dim.GlobalVariable;
import chat.dim.Register;
import chat.dim.SharedFacebook;
import chat.dim.group.SharedGroupManager;
import chat.dim.mkm.User;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.group.expel.ExpelActivity;
import chat.dim.sechat.group.invite.InviteActivity;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.threading.MainThread;
import chat.dim.type.Pair;
import chat.dim.ui.Alert;

public class ParticipantsAdapter extends ArrayAdapter<ID> {

    public static final ID INVITE_BTN_ID = ID.parse("invite@anywhere");
    public static final ID EXPEL_BTN_ID = ID.parse("expel@anywhere");

    private final int resId;
    private final ID identifier;

    public ParticipantsAdapter(Context context, int resource, List<ID> objects, ID conversation) {
        super(context, resource, objects);
        resId = resource;
        identifier = conversation;
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
            viewHolder.cardView = view.findViewById(R.id.cardView);
            viewHolder.avatarView = view.findViewById(R.id.avatarView);
            viewHolder.nameView = view.findViewById(R.id.nameView);
            viewHolder.inviteButton = view.findViewById(R.id.inviteButton);
            viewHolder.expelButton = view.findViewById(R.id.expelButton);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.showParticipant(identifier);

        return view;
    }

    private void invite() {
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        User user = facebook.getCurrentUser();
        if (user == null) {
            Alert.tips(getContext(), "Current user not found");
            return;
        }

        ID group;
        if (identifier.isGroup()) {
            SharedGroupManager manager = SharedGroupManager.getInstance();
            List<ID> allMembers = manager.getMembers(identifier);
            if (!allMembers.contains(user.getIdentifier())) {
                Alert.tips(getContext(), "You are not a member of this group: " + identifier);
                return;
            }
            group = identifier;
        } else {
            Register register = new Register(facebook.getDatabase());
            group = register.createGroup(user.getIdentifier(), "Sophon Shield");
        }

        // open Invite activity
        Context context = getContext();
        Intent intent = new Intent();
        intent.setClass(context, InviteActivity.class);
        intent.putExtra("ID", group.toString());
        intent.putExtra("from", identifier.toString());
        context.startActivity(intent);
    }

    private void expel() {
        GlobalVariable shared = GlobalVariable.getInstance();
        SharedFacebook facebook = shared.facebook;
        User user = facebook.getCurrentUser();
        if (user == null) {
            Alert.tips(getContext(), "Current user not found");
            return;
        }

        SharedGroupManager manager = SharedGroupManager.getInstance();
        if (!manager.isOwner(user.getIdentifier(), identifier)) {
            Alert.tips(getContext(), "You are not admin of this group: " + identifier);
            return;
        }

        // open Expel activity
        Context context = getContext();
        Intent intent = new Intent();
        intent.setClass(context, ExpelActivity.class);
        intent.putExtra("ID", identifier.toString());
        context.startActivity(intent);
    }

    private void showProfile(ID identifier) {
        Intent intent = new Intent();
        intent.setClass(getContext(), ProfileActivity.class);
        intent.putExtra("ID", identifier.toString());
        getContext().startActivity(intent);
    }

    class ViewHolder implements Observer {

        CardView cardView = null;

        ImageView avatarView = null;
        TextView nameView = null;
        ImageButton inviteButton = null;
        ImageButton expelButton = null;

        private ID identifier = null;

        ViewHolder() {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.addObserver(this, NotificationNames.DocumentUpdated);
            nc.addObserver(this, NotificationNames.FileDownloadSuccess);
        }

        @Override
        protected void finalize() throws Throwable {
            NotificationCenter nc = NotificationCenter.getInstance();
            nc.removeObserver(this, NotificationNames.DocumentUpdated);
            nc.removeObserver(this, NotificationNames.FileDownloadSuccess);
            super.finalize();
        }

        @Override
        public void onReceiveNotification(Notification notification) {
            String name = notification.name;
            Map<String, Object> info = notification.userInfo;
            assert name != null && info != null : "notification error: " + notification;
            if (name.equals(NotificationNames.DocumentUpdated)) {
                ID user = ID.parse(info.get("ID"));
                if (identifier.equals(user)) {
                    MainThread.call(this::refresh);
                }
            } else if (name.equals(NotificationNames.FileDownloadSuccess)) {
                GlobalVariable shared = GlobalVariable.getInstance();
                SharedFacebook facebook = shared.facebook;
                Pair<String, URL> avatars = facebook.getAvatar(identifier);
                String path = (String) info.get("path");
                if (avatars.first != null && avatars.first.equals(path)) {
                    MainThread.call(this::refresh);
                }
            }
        }

        private void refresh() {
            GlobalVariable shared = GlobalVariable.getInstance();
            SharedFacebook facebook = shared.facebook;
            String nickname = facebook.getName(identifier);
            nameView.setText(nickname);

            Bitmap avatar = UserViewModel.getAvatar(identifier);
            avatarView.setImageBitmap(avatar);
        }

        private void showParticipant(ID id) {
            identifier = id;

            if (INVITE_BTN_ID.equals(identifier)) {
                // invite
                cardView.setVisibility(View.GONE);
                avatarView.setVisibility(View.GONE);
                nameView.setVisibility(View.GONE);
                inviteButton.setVisibility(View.VISIBLE);
                inviteButton.setOnClickListener(v -> invite());
                expelButton.setVisibility(View.GONE);
                return;
            }
            if (EXPEL_BTN_ID.equals(identifier)) {
                // expel
                cardView.setVisibility(View.GONE);
                avatarView.setVisibility(View.GONE);
                nameView.setVisibility(View.GONE);
                inviteButton.setVisibility(View.GONE);
                expelButton.setVisibility(View.VISIBLE);
                expelButton.setOnClickListener(v -> expel());
                return;
            }

            cardView.setVisibility(View.VISIBLE);
            cardView.setOnClickListener(v -> showProfile(identifier));

            avatarView.setVisibility(View.VISIBLE);
            nameView.setVisibility(View.VISIBLE);

            inviteButton.setVisibility(View.GONE);
            expelButton.setVisibility(View.GONE);

            refresh();
        }
    }
}
