package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModelProviders;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import chat.dim.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.SechatApp;

public class UpdateAccountFragment extends Fragment {

    private AccountViewModel mViewModel;

    private ImageButton avatarButton;
    private EditText nicknameText;
    private TextView numberView;
    private TextView addressView;

    public static UpdateAccountFragment newInstance() {
        return new UpdateAccountFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.update_account_fragment, container, false);

        avatarButton = view.findViewById(R.id.avatar);
        nicknameText = view.findViewById(R.id.nickname);
        numberView = view.findViewById(R.id.number);
        addressView = view.findViewById(R.id.address);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);

        // avatar
        Uri avatar = mViewModel.getAvatarUrl();
        avatarButton.setImageURI(avatar);

        // nickname
        String nickname = mViewModel.getNickname();
        nicknameText.setText(nickname);

        // ID.number & address
        String number = mViewModel.getNumberString();
        numberView.setText(number);
        ID identifier = mViewModel.getIdentifier();
        if (identifier != null) {
            addressView.setText(identifier.address.toString());
        }

        if (nickname != null) {
            getActivity().setTitle(nickname);
        }
    }

}
