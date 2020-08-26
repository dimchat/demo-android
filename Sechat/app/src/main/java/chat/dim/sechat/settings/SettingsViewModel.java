package chat.dim.sechat.settings;

import androidx.lifecycle.ViewModel;

import chat.dim.ID;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.sqlite.sp.ProviderTable;

public class SettingsViewModel extends ViewModel {

    ID getID(String string) {
        return EntityViewModel.getID(string);
    }

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
