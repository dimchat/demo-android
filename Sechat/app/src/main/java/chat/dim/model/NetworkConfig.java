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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.common.Facebook;
import chat.dim.database.NetworkDatabase;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;

public class NetworkConfig extends NetworkDatabase {
    private static final NetworkConfig ourInstance = new NetworkConfig();
    public static NetworkConfig getInstance() { return ourInstance; }
    private NetworkConfig() {
        super();
    }

    static {

        NetworkConfig networkConfig = NetworkConfig.getInstance();

        // FIXME: test SP

        // sp ID
        ID spID = ID.ANYONE;

        List<String> providers = new ArrayList<>();
        providers.add(spID.toString());
        networkConfig.saveProviders(providers);

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
            Facebook facebook = Facebook.getInstance();
            facebook.saveMeta(meta, stationID);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        // station config
        Map<String, Object> srvConfig = new HashMap<>();
        srvConfig.put("ID", stationID);
        srvConfig.put("host", host);
        srvConfig.put("port", port);

        // station list
        List<Map<String, Object>> stations = new ArrayList<>();
        stations.add(srvConfig);
        networkConfig.saveStations(stations, spID);
    }
}
