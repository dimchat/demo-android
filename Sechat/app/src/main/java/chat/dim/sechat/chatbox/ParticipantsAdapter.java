package chat.dim.sechat.chatbox;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import chat.dim.Group;
import chat.dim.ID;
import chat.dim.User;
import chat.dim.extension.Register;
import chat.dim.model.Facebook;
import chat.dim.sechat.R;
import chat.dim.sechat.group.InviteActivity;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.sechat.profile.ProfileActivity;
import chat.dim.ui.Alert;

class ParticipantsAdapter extends ArrayAdapter<ID> {

    private final int resId;
    private final ID identifier;

    ParticipantsAdapter(Context context, int resource, List<ID> objects, ID conversation) {
        super(context, resource, objects);
        resId = resource;
        identifier = conversation;
    }

    @SuppressWarnings("NullableProblems")
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
        showParticipant(identifier, viewHolder);

        return view;
    }

    private void showParticipant(ID identifier, ViewHolder viewHolder) {

        if (ChatManageViewModel.INVITE_BTN_ID.equals(identifier)) {
            // invite
            viewHolder.cardView.setVisibility(View.GONE);
            viewHolder.avatarView.setVisibility(View.GONE);
            viewHolder.nameView.setVisibility(View.GONE);
            viewHolder.inviteButton.setVisibility(View.VISIBLE);
            viewHolder.inviteButton.setOnClickListener(v -> invite());
            viewHolder.expelButton.setVisibility(View.GONE);
            return;
        }
        if (ChatManageViewModel.EXPEL_BTN_ID.equals(identifier)) {
            // expel
            viewHolder.cardView.setVisibility(View.GONE);
            viewHolder.avatarView.setVisibility(View.GONE);
            viewHolder.nameView.setVisibility(View.GONE);
            viewHolder.inviteButton.setVisibility(View.GONE);
            viewHolder.expelButton.setVisibility(View.VISIBLE);
            viewHolder.expelButton.setOnClickListener(v -> expel());
            return;
        }

        viewHolder.cardView.setVisibility(View.VISIBLE);
        viewHolder.cardView.setOnClickListener(v -> showParticipant(identifier));

        // avatar
        if (viewHolder.avatarView != null) {
            viewHolder.avatarView.setVisibility(View.VISIBLE);

            Uri avatar = UserViewModel.getAvatarUri(identifier);
            viewHolder.avatarView.setImageURI(avatar);
        }

        // name
        if (viewHolder.nameView != null) {
            viewHolder.nameView.setVisibility(View.VISIBLE);

            String name = UserViewModel.getNickname(identifier);
            viewHolder.nameView.setText(name);
        }

        if (viewHolder.inviteButton != null) {
            viewHolder.inviteButton.setVisibility(View.GONE);
        }

        if (viewHolder.expelButton != null) {
            viewHolder.expelButton.setVisibility(View.GONE);
        }
    }

    private void invite() {
        Facebook facebook = Facebook.getInstance();
        User user = facebook.getCurrentUser();
        if (user == null) {
            Alert.tips(getContext(), "Current user not found");
            return;
        }
        Group group;
        if (identifier.isGroup()) {
            if (!facebook.existsMember(user.identifier, identifier)) {
                Alert.tips(getContext(), "You are not a member of this group: " + identifier);
                return;
            }
            group = facebook.getGroup(identifier);
        } else {
            assert identifier.isUser() : "user ID error: " + identifier;
            Register register = new Register();
            group = register.createGroup(user.identifier, "Sophon Shield");
            facebook.addMember(identifier, group.identifier);
        }
        // open Invite activity
        Context context = getContext();
        Intent intent = new Intent();
        intent.setClass(context, InviteActivity.class);
        intent.putExtra("ID", group.identifier.toString());
        context.startActivity(intent);
    }

    private void expel() {
        Facebook facebook = Facebook.getInstance();
        User user = facebook.getCurrentUser();
        if (!ChatManageViewModel.isAdmin(user, identifier)) {
            Alert.tips(getContext(), "You are not admin of this group: " + identifier);
            return;
        }
        Group group = facebook.getGroup(identifier);
    }

    private void showParticipant(ID identifier) {
        Intent intent = new Intent();
        intent.setClass(getContext(), ProfileActivity.class);
        intent.putExtra("ID", identifier.toString());
        getContext().startActivity(intent);
    }

    static class ViewHolder {

        CardView cardView = null;

        ImageView avatarView = null;
        TextView nameView = null;
        ImageButton inviteButton = null;
        ImageButton expelButton = null;
    }
}
