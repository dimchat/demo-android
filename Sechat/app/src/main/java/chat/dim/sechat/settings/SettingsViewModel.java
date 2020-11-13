package chat.dim.sechat.settings;

import androidx.lifecycle.ViewModel;

import chat.dim.protocol.ID;
import chat.dim.sqlite.dim.ProviderTable;

public class SettingsViewModel extends ViewModel {

    String getCurrentProviderName() {
        return StationViewModel.getCurrentProviderName();
    }

    String getCurrentStationName() {
        return StationViewModel.getCurrentStationName();
    }

    ProviderTable.StationInfo getCurrentStationInfo() {
        return StationViewModel.getCurrentStationInfo();
    }

    boolean addStation(ID station, String host, int port) {
        return StationViewModel.addStation(station, host, port);
    }
}
