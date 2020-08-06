package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import chat.dim.model.Configuration;
import chat.dim.sechat.R;
import chat.dim.ui.web.WebViewActivity;

public class AccountFragment extends Fragment {

    private AccountViewModel mViewModel;

    private ImageView avatarView;
    private TextView nameView;
    private TextView descView;

    private ImageButton detailButton;

    private Button termsButton;
    private Button aboutButton;

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

        detailButton = view.findViewById(R.id.detailBtn);
        termsButton = view.findViewById(R.id.termBtn);
        aboutButton = view.findViewById(R.id.aboutBtn);

        FragmentActivity activity = getActivity();
        assert activity != null : "should not happen";
        activity.setTitle(R.string.me);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);

        nameView.setText(mViewModel.getUserTitle());
        descView.setText(mViewModel.getAddressString());

        avatarView.setImageBitmap(mViewModel.getAvatar());

        detailButton.setOnClickListener(v -> detail());
        termsButton.setOnClickListener(v -> open(R.string.terms, config.getTermsURL()));
        aboutButton.setOnClickListener(v -> open(R.string.about, config.getAboutURL()));
    }

    private void detail() {
        Context context = getContext();
        assert context != null : "failed to get context";
        Intent intent = new Intent();
        intent.setClass(context, UpdateAccountActivity.class);
        startActivity(intent);
    }

    private void open(int resId, String url) {
        String title = (String) getText(resId);
        WebViewActivity.open(getActivity(), title, url);
    }
}
