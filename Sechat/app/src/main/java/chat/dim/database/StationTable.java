package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;

public class StationTable extends ExternalStorage {

    // "/sdcard/chat.dim.sechat/dim/{SP_ADDRESS}/stations.js"

    private static String getStationsFilePath(ID sp) {
        return root + "/dim/" + sp.address + "/stations.js";
    }

    public static boolean saveStations(List stations, ID sp) {
        String path = getStationsFilePath(sp);
        try {
            return writeJSON(stations, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static List<Map> allStations(ID sp) {
        String path = getStationsFilePath(sp);
        try {
            return (List<Map>) readJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    //-------- Service Provider

    // "/sdcard/chat.dim.sechat/dim/{SP_ADDRESS}/config.js"

    private static String getConfigFilePath(ID sp) {
        return root + "/dim/" + sp.address + "/config.js";
    }

    @SuppressWarnings("unchecked")
    public static Map getProviderConfig(ID sp) {
        String path = getConfigFilePath(sp);
        Map<String, Object> config = null;
        try {
            config = (Map<String, Object>) readJSON(path);
        } catch (IOException e) {
            //e.printStackTrace();
        }
        if (config == null) {
            config = new HashMap<>();
            config.put("ID", sp);
        }
        List<Map> stations = allStations(sp);
        if (stations != null) {
            config.put("stations", stations);
        }
        return config;
    }

    // "/sdcard/chat.dim.sechat/dim/service_providers.js"

    private static String getProvidersFilePath() {
        return root + "/dim/service_providers.js";
    }

    @SuppressWarnings("unchecked")
    public static List<Map> allProviders() {
        String path = getProvidersFilePath();
        try {
            return (List<Map>) readJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean saveProviders(List providers) {
        String path = getProvidersFilePath();
        try {
            return writeJSON(providers, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    static {

        // FIXME: test SP

        // sp ID
        ID spID = ID.ANYONE;

        List<ID> providers = new ArrayList<>();
        providers.add(spID);
        saveProviders(providers);

        // FIXME: test station(s)

        // station IP
//        String host = "127.0.0.1";
        String host = "134.175.87.98";

        // station Port
        int port = 9394;

        // station ID
        ID stationID = ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");

        // station meta
        Map<String, Object> keyDict = new HashMap<>();
        keyDict.put("algorithm", "RSA");
        keyDict.put("data", "-----BEGIN PUBLIC KEY-----\n" +
                "MIGJAoGBAMRPt+8u6lQFRzoibAl6MKiXJuqtT5jo/CIIqFuUZvBWpu9qkPYDWGMS7gS0vqabe/uF\n" +
                "yCw7cN/ff9KFG5roZb38lBUMt93Oct+5ODzDlSD53Kwl9uuYHUbuduoNgTJdBc1FalCwsdhIP+KF\n" +
                "pIpY2c65XRzuJ4kTNACn74753dZRAgMBAAE\n" +
                "-----END PUBLIC KEY-----\n");
        Map<String, Object> metaDict = new HashMap<>();
        metaDict.put("version", 1);
        metaDict.put("seed", "gsp-s001");
        metaDict.put("key", keyDict);
        metaDict.put("fingerprint", "R+Bv3RlVi8pNuVWDJ8uEp+N3l+B04ftlaNFxo7u8+V6eSQsQJNv7tfQNFdC633UpXDw3zZHvQNnkUBwthaCJTbEmy2CYqMSx/BLuaS7spkSZJJAT7++xqif+pRjdw9yM/aPufKHS4PAvGec21PsUpQzOV5TQFyF5CDEDVLC8HVY=");
        try {
            Meta meta = Meta.getInstance(metaDict);
            SocialNetworkDatabase userDB = SocialNetworkDatabase.getInstance();
            userDB.saveMeta(meta, stationID);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // station config
        Map<String, Object> srvConfig = new HashMap<>();
        srvConfig.put("ID", stationID);
        srvConfig.put("meta", metaDict);
        srvConfig.put("host", host);
        srvConfig.put("port", port);

        // station list
        List<Map> stations = new ArrayList<>();
        stations.add(srvConfig);
        saveStations(stations, spID);
    }
}
