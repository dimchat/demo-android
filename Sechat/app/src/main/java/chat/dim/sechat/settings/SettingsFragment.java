package chat.dim.sechat.settings;

import androidx.lifecycle.ViewModelProviders;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableRow;
import android.widget.TextView;

import chat.dim.sechat.R;
import chat.dim.ui.Alert;

public class SettingsFragment extends Fragment {

    private SettingsViewModel mViewModel;

    private TextView currentProvider;
    private TextView currentStation;

    private TableRow providerRow;
    private TableRow stationRow;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_fragment, container, false);

        currentProvider = view.findViewById(R.id.currentProvider);
        currentStation = view.findViewById(R.id.currentStation);

        providerRow = view.findViewById(R.id.providerRow);
        stationRow = view.findViewById(R.id.stationRow);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        // TODO: Use the ViewModel

        String spName = mViewModel.getCurrentProviderName();
        currentProvider.setText(spName);

        String sName = mViewModel.getCurrentStationName();
        currentStation.setText(sName);

        providerRow.setOnClickListener(v -> selectProvider());
        stationRow.setOnClickListener(v -> selectStation());
    }

    private void selectProvider() {
        Alert.tips(getContext(), "Not implement yet!");
    }

    private void selectStation() {
        Context context = getContext();
        assert context != null : "failed to get context";
        Intent intent = new Intent();
        intent.setClass(context, SettingStationActivity.class);
        startActivity(intent);
    }
}
