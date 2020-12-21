package chat.dim.sechat.account.modify;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import chat.dim.client.Messenger;
import chat.dim.protocol.ID;
import chat.dim.sechat.R;
import chat.dim.sechat.account.AccountViewModel;

public class ExportFragment extends Fragment {

    private AccountViewModel mViewModel;

    public ExportFragment() {
        super();
        // Required empty public constructor
    }

    public static ExportFragment newInstance(AccountViewModel viewModel) {
        ExportFragment fragment = new ExportFragment();
        fragment.mViewModel = viewModel;
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.account_export_fragment, container, false);

        ImageButton closeButton = view.findViewById(R.id.close);
        closeButton.setOnClickListener(v -> close());

        TextView textView = view.findViewById(R.id.userInfo);
        textView.setText(mViewModel.serializePrivateInfo());

        // backup contacts(encrypted) to DIM station
        List<ID> contacts = mViewModel.getContacts();
        if (contacts != null && contacts.size() > 0) {
            Messenger messenger = Messenger.getInstance();
            messenger.postContacts(contacts);
        }

        return view;
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
