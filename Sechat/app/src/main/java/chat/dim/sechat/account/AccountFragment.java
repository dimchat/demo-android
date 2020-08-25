package chat.dim.sechat.account;

import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;

import chat.dim.model.Configuration;
import chat.dim.sechat.R;
import chat.dim.sechat.settings.SettingsActivity;
import chat.dim.ui.web.WebViewActivity;

public class AccountFragment extends Fragment {

    private AccountViewModel mViewModel;

    private ImageView avatarView;
    private TextView nameView;
    private TextView descView;

    private LinearLayout detail;

    private TableRow settingsButton;

    private TableRow termsButton;
    private TableRow aboutButton;

    public static AccountFragment newInstance() {
        return new AccountFragment();
    }

    private Configuration config = Configuration.getInstance();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.account_fragment, container, false);

        avatarView = view.findViewById(R.id.avatarView);
        nameView = view.findViewById(R.id.nameView);
        descView = view.findViewById(R.id.descView);

        detail = view.findViewById(R.id.detail);

        settingsButton = view.findViewById(R.id.settingsRow);

        termsButton = view.findViewById(R.id.termsRow);
        aboutButton = view.findViewById(R.id.aboutRow);

        FragmentActivity activity = getActivity();
        assert activity != null : "should not happen";
        activity.setTitle(R.string.main_me);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);

        nameView.setText(mViewModel.getUserTitle());
        descView.setText(mViewModel.getAddressString());

        avatarView.setImageBitmap(mViewModel.getAvatar());

        detail.setOnClickListener(v -> showDetail());

        settingsButton.setOnClickListener(v -> showSettings());

        termsButton.setOnClickListener(v -> open(R.string.terms, config.getTermsURL()));
        aboutButton.setOnClickListener(v -> open(R.string.about, config.getAboutURL()));
    }

    private void showDetail() {
        Context context = getContext();
        assert context != null : "failed to get context";
        Intent intent = new Intent();
        intent.setClass(context, UpdateAccountActivity.class);
        startActivity(intent);
    }

    private void showSettings() {
        Context context = getContext();
        assert context != null : "failed to get context";
        Intent intent = new Intent();
        intent.setClass(context, SettingsActivity.class);
        startActivity(intent);
    }

    private void open(int resId, String url) {
        String title = (String) getText(resId);
        WebViewActivity.open(getActivity(), title, url);
    }
}
