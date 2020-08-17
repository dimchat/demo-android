package chat.dim.sechat.account;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.List;

import chat.dim.ID;
import chat.dim.model.Messenger;
import chat.dim.sechat.R;

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
