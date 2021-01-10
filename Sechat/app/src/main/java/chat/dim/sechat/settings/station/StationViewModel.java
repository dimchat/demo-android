package chat.dim.sechat.settings.station;

import java.util.List;

import chat.dim.model.NetworkDatabase;
import chat.dim.protocol.ID;
import chat.dim.sechat.model.UserViewModel;
import chat.dim.sqlite.dim.ProviderTable;
import chat.dim.utils.Log;

public class StationViewModel extends UserViewModel {

    private static NetworkDatabase database = NetworkDatabase.getInstance();

    private static ID spid = null;
    private static List<ProviderTable.StationInfo> stations = null;

    static ID getCurrentProvider() {
        if (spid == null) {
            List<ProviderTable.ProviderInfo> providers = database.allProviders();
            if (providers == null || providers.size() == 0) {
                return null;
            }
            ProviderTable.ProviderInfo first = providers.get(0);
            spid = first.identifier;
        }
        return spid;
    }

    public static String getCurrentProviderName() {
        if (spid == null) {
            if (getCurrentProvider() == null) {
                return null;
            }
        }
        return getFacebook().getName(spid);
    }

    public static ProviderTable.StationInfo getCurrentStationInfo() {
        if (spid == null) {
            if (getCurrentProvider() == null) {
                return null;
            }
        }
        List<ProviderTable.StationInfo> stations = getStations(spid);
        if (stations == null || stations.size() == 0) {
            return null;
        }
        return stations.get(0);
    }

    public static String getCurrentStationName() {
        ProviderTable.StationInfo info = getCurrentStationInfo();
        if (info == null || info.identifier == null) {
            return null;
        }
        return getFacebook().getName(info.identifier);
    }

    static List<ProviderTable.StationInfo> getStations(ID sp) {
        if (spid == null) {
            spid = sp;
            stations = database.allStations(sp);
            return stations;
        } else if (spid.equals(sp)) {
            if (stations == null) {
                stations = database.allStations(sp);
            }
            return stations;
        } else {
            return database.allStations(sp);
        }
    }

    public static boolean addStation(ID station, String host, int port) {
        ID sp = getCurrentProvider();
        if (sp == null) {
            throw new NullPointerException("current SP not found");
        }
        List<ProviderTable.StationInfo> array = getStations(spid);
        if (array != null && array.size() > 0) {
            for (ProviderTable.StationInfo item : array) {
                if (station.equals(item.identifier)) {
                    Log.error("duplication station: " + station + ", SP=" + sp);
                    return false;
                }
            }
        }
        stations = null;
        return database.addStation(sp, station, host, port, getFacebook().getName(station), 0);
    }

    static boolean chooseStation(ID station) {
        ID sp = getCurrentProvider();
        if (sp == null) {
            throw new NullPointerException("current SP not found");
        }
        stations = null;
        return database.chooseStation(sp, station);
    }

    static boolean deleteStation(ID station, String host, int port) {
        ID sp = getCurrentProvider();
        if (sp == null) {
            throw new NullPointerException("current SP not found");
        }
        stations = null;
        return database.removeStation(sp, station, host, port);
    }
}
