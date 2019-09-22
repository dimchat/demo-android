package chat.dim.sechat.profile;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.mkm.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.chatbox.ChatboxActivity;

public class ProfileFragment extends Fragment {

    private ProfileViewModel mViewModel;

    private ID identifier;

    private ImageView imageView;
    private TextView seedView;
    private TextView addressView;
    private TextView numberView;

    private Button addButton;
    private Button messageButton;

    public static ProfileFragment newInstance(ID identifier) {
        ProfileFragment object = new ProfileFragment();
        object.identifier = identifier;
        return object;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);

        imageView = view.findViewById(R.id.imageView);
        seedView = view.findViewById(R.id.seed);
        addressView = view.findViewById(R.id.address);
        numberView = view.findViewById(R.id.number);

        addButton = view.findViewById(R.id.addContact);
        messageButton = view.findViewById(R.id.sendMessage);

        messageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert getContext() != null;
                Intent intent = new Intent();
                intent.setClass(getContext(), ChatboxActivity.class);
                intent.putExtra("ID", identifier.toString());
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(ProfileViewModel.class);
        // TODO: Use the ViewModel

        seedView.setText(identifier.name);
        addressView.setText(identifier.address.toString());
        numberView.setText(mViewModel.getNumberString(identifier));

        if (mViewModel.existsContact(identifier)) {
            addButton.setVisibility(View.INVISIBLE);
            messageButton.setVisibility(View.VISIBLE);
        } else {
            addButton.setVisibility(View.VISIBLE);
            messageButton.setVisibility(View.INVISIBLE);
        }
    }

}
