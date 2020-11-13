package chat.dim.sechat.register;

import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;

import chat.dim.User;
import chat.dim.model.Facebook;
import chat.dim.model.Messenger;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.account.AccountViewModel;
import chat.dim.ui.Alert;

public class ImportFragment extends Fragment {

    private AccountViewModel mViewModel;

    private EditText privateInfo;

    private ImageButton closeButton;
    private Button importButton;

    public static ImportFragment newInstance() {
        return new ImportFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.register_import_fragment, container, false);

        privateInfo = view.findViewById(R.id.privateInfo);

        closeButton = view.findViewById(R.id.close);
        importButton = view.findViewById(R.id.importButton);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        // TODO: Use the ViewModel

        closeButton.setOnClickListener(v -> close());
        importButton.setOnClickListener(v -> checkImport());
    }

    private void checkImport() {
        RegisterActivity activity = (RegisterActivity) getActivity();
        assert activity != null : "failed to get register activity";

        String info = privateInfo.getText().toString();
        ID identifier = mViewModel.savePrivateInfo(info);
        if (identifier == null) {
            Alert.tips(activity, R.string.register_account_error);
            return;
        }

        Facebook facebook = Facebook.getInstance();
        User user = facebook.getUser(identifier);
        if (user == null) {
            Alert.tips(activity, R.string.register_account_error);
            return;
        }
        facebook.setCurrentUser(user);

        Messenger messenger = Messenger.getInstance();
        messenger.queryContacts();

        activity.close();
    }

    private void close() {
        FragmentActivity activity = getActivity();
        assert activity != null : "should not happen";
        FragmentManager fragmentManager = activity.getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(this);
        transaction.commit();
    }
}
