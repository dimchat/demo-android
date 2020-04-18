/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.model;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Meta;
import chat.dim.database.ProviderTable;
import chat.dim.database.StationTable;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;

public class NetworkDatabase {
    private static final NetworkDatabase ourInstance = new NetworkDatabase();
    public static NetworkDatabase getInstance() { return ourInstance; }
    private NetworkDatabase() {
        super();
    }

    private ProviderTable providerTable = new ProviderTable();
    private StationTable stationTable = new StationTable();

    /**
     *  Get all service providers
     *
     * @return provider ID list
     */
    public List<String> allProviders() {
        return providerTable.allProviders();
    }

    /**
     *  Get provider config with ID
     *
     * @param sp - sp ID
     * @return config
     */
    public Map<String, Object> getProviderConfig(ID sp) {
        Map<String, Object> config = providerTable.getProviderConfig(sp);
        Object stations = config.get("stations");
        if (stations == null) {
            stations = allStations(sp);
            if (stations != null) {
                config.put("stations", stations);
            }
        }
        return config;
    }

    /**
     *  Save provider list
     *
     * @param providers - provider ID list
     * @return true on success
     */
    public boolean saveProviders(List<String> providers) {
        return providerTable.saveProviders(providers);
    }

    //-------- Station

    /**
     *  Get all stations under the service provider
     *
     * @param sp - sp ID
     * @return station config list
     */
    public List<Map<String, Object>> allStations(ID sp) {
        return stationTable.allStations(sp);
    }

    /**
     *  Save station config list for the service provider
     *
     * @param stations - station config list
     * @param sp - sp ID
     * @return true on success
     */
    public boolean saveStations(List<Map<String, Object>> stations, ID sp) {
        return stationTable.saveStations(stations, sp);
    }

    /**
     *  Resource Loader for built-in accounts
     */
    private static class ResourceLoader {

        static Map loadJSON(String filename) throws IOException {
            byte[] data = loadData(filename);
            return (Map) JSON.decode(data);
        }

        private static String loadText(String filename) throws IOException {
            byte[] data = loadData(filename);
            return UTF8.decode(data);
        }

        private static byte[] loadData(String filename) throws IOException {
            InputStream is = ResourceLoader.class.getClassLoader().getResourceAsStream(filename);
            assert is != null : "failed to get resource: " + filename;
            int size = is.available();
            byte[] data = new byte[size];
            int len = is.read(data, 0, size);
            assert len == size : "reading resource error: " + len + " != " + size;
            return data;
        }
    }

    private static Meta loadMeta(ID identifier) {
        try {
            Map meta = ResourceLoader.loadJSON(identifier.address + "/meta.js");
            return Meta.getInstance(meta);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    static {

        NetworkDatabase database = NetworkDatabase.getInstance();

        // FIXME: test SP

        // sp ID
        ID spID = ID.ANYONE;

        List<String> providers = new ArrayList<>();
        providers.add(spID.toString());
        database.saveProviders(providers);

        // FIXME: test station(s)

        // station IP
//        String host = "127.0.0.1";
        String host = "134.175.87.98";

        // station Port
        int port = 9394;

        // station ID
//        ID stationID = ID.getInstance("gsp-s001@x5Zh9ixt8ECr59XLye1y5WWfaX4fcoaaSC");
        ID stationID = ID.getInstance("gsp-s002@wpjUWg1oYDnkHh74tHQFPxii6q9j3ymnyW");

        Meta meta = loadMeta(stationID);
        Facebook facebook = Facebook.getInstance();
        facebook.saveMeta(meta, stationID);

        // station config
        Map<String, Object> srvConfig = new HashMap<>();
        srvConfig.put("ID", stationID);
        srvConfig.put("host", host);
        srvConfig.put("port", port);

        // station list
        List<Map<String, Object>> stations = new ArrayList<>();
        stations.add(srvConfig);
        database.saveStations(stations, spID);
    }
}
