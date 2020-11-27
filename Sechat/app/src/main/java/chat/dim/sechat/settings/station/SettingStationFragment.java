package chat.dim.sechat.settings.station;

import androidx.lifecycle.ViewModelProviders;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Map;

import chat.dim.Entity;
import chat.dim.notification.Notification;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.notification.Observer;
import chat.dim.protocol.ID;
import chat.dim.protocol.NetworkType;
import chat.dim.sechat.Client;
import chat.dim.sechat.R;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sechat.settings.SettingsViewModel;
import chat.dim.sqlite.dim.ProviderTable;
import chat.dim.ui.Alert;
import chat.dim.ui.list.ListFragment;

public class SettingStationFragment extends ListFragment<StationViewAdapter, StationList> implements Observer {

    private SettingsViewModel mViewModel;

    private EditText newSID = null;
    private EditText newHost = null;
    private EditText newPort = null;

    private Button addButton = null;

    private TextView currentHost = null;
    private TextView currentPort = null;
    private TextView currentName = null;

    public static SettingStationFragment newInstance() {
        return new SettingStationFragment();
    }

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public SettingStationFragment() {
        super();

        dummyList = new StationList();
        adapter = new StationViewAdapter(dummyList, null);

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.addObserver(this, NotificationNames.ServiceProviderUpdated);
    }

    @Override
    public void onDestroy() {
        // reconnect to new station
        Client client = Client.getInstance();
        client.startServer();

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.removeObserver(this, NotificationNames.ServiceProviderUpdated);
        super.onDestroy();
    }

    @Override
    public void onReceiveNotification(Notification notification) {
        String name = notification.name;
        Map info = notification.userInfo;
        assert name != null && info != null : "notification error: " + notification;
        if (name.equals(NotificationNames.ServiceProviderUpdated)) {
            reloadData();

            String sid = newSID.getText().toString();
            String action = (String) info.get("action");
            if ("remove".equals(action) && sid.length() == 0) {
                ID station = (ID) info.get("station");
                String host = (String) info.get("host");
                String port = "" + info.get("port");
                assert station != null : "station ID should not be empty";
                newSID.setText(station.toString());
                newHost.setText(host);
                newPort.setText(port);
            }
        }
    }

    @Override
    public void reloadData() {
        super.reloadData();

        // TODO: check current SP name

        ProviderTable.StationInfo first = mViewModel.getCurrentStationInfo();
        assert first != null : "current station not found";
        showCurrentStation(first);
    }

    private void showCurrentStation(ProviderTable.StationInfo stationInfo) {
        String host = stationInfo.host;
        String port = "" + stationInfo.port;
        String name = EntityViewModel.getName(stationInfo.identifier);

        currentHost.setText(host);
        currentPort.setText(port);
        currentName.setText(name);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.setting_station_fragment, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.station_list);
        bindRecyclerView(recyclerView); // Set the adapter

        newSID = view.findViewById(R.id.new_id);
        newHost = view.findViewById(R.id.new_host);
        newPort = view.findViewById(R.id.new_port);
        addButton = view.findViewById(R.id.add);

        currentHost = view.findViewById(R.id.current_host);
        currentPort = view.findViewById(R.id.current_port);
        currentName = view.findViewById(R.id.current_name);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(this).get(SettingsViewModel.class);
        // TODO: Use the ViewModel

        reloadData();

        addButton.setOnClickListener(v -> addStation());
    }

    private void addStation() {
        String sid = newSID.getText().toString();
        ID identifier = Entity.parseID(sid);
        if (identifier == null || !NetworkType.Station.equals(identifier.getType())) {
            Alert.tips(getContext(), "Station ID error: " + sid);
            return;
        }
        String host = newHost.getText().toString();
        String port = newPort.getText().toString();
        if (mViewModel.addStation(identifier, host, Integer.parseInt(port))) {
            Alert.tips(getContext(), "station " + identifier + "(" + host + ":" + port + ") added");
        } else {
            Alert.tips(getContext(), "Failed to add station: " + identifier + " (" + host + ":" + port + ")");
        }
    }
}
