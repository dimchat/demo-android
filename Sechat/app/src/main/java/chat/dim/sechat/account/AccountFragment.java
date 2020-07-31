package chat.dim.sechat.account;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
import chat.dim.ui.Resources;
import chat.dim.ui.WebViewActivity;

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
        detailButton.setOnClickListener(v -> detail());

        termsButton = view.findViewById(R.id.termBtn);
        termsButton.setOnClickListener(v -> open(R.string.terms, config.getTermsURL()));

        aboutButton = view.findViewById(R.id.aboutBtn);
        aboutButton.setOnClickListener(v -> open(R.string.about, config.getAboutURL()));

        FragmentActivity activity = getActivity();
        assert activity != null : "should not happen";
        activity.setTitle(R.string.me);

        return view;
    }

    private void detail() {
        Intent intent = new Intent();
        intent.setClass(getContext(), UpdateAccountActivity.class);
        startActivity(intent);
    }

    private void open(int resId, String url) {
        String title = (String) Resources.getText(getContext(), resId);
        open(title, url);
    }

    private void open(String title, String url) {
        Intent intent = new Intent();
        intent.setClass(getContext(), WebViewActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("URL", url);
        startActivity(intent);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(AccountViewModel.class);
        // TODO: Use the ViewModel

        nameView.setText(mViewModel.getUserTitle());
        descView.setText(mViewModel.getAddressString());

        Bitmap avatar = mViewModel.getAvatar();
        avatarView.setImageBitmap(avatar);
    }

}
