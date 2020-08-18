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
package chat.dim.database;

import java.util.List;

import chat.dim.ID;

public interface ProviderTable {

    class ProviderInfo {
        public ID identifier;
        public String name;
        public String url;
        public int chosen;

        public ProviderInfo(ID identifier, String name, String url, int chosen) {
            this.identifier = identifier;
            this.name = name;
            this.url = url;
            this.chosen = chosen;
        }
    }

    List<ProviderInfo> getProviders();

    boolean addProvider(ID identifier, String name, String url, int chosen);

    boolean updateProvider(ID identifier, String name, String url, int chosen);

    boolean removeProvider(ID identifier);

    //
    //  Stations
    //

    class StationInfo {
        public ID identifier;
        public String name;
        public String host;
        public int port;
        public int chosen;

        public StationInfo(ID identifier, String name, String host, int port, int chosen) {
            this.identifier = identifier;
            this.name = name;
            this.host = host;
            this.port = port;
            this.chosen = chosen;
        }
    }

    List<StationInfo> getStations(ID sp);

    boolean addStation(ID sp, ID station, String host, int port, String name, int chosen);

    boolean updateStation(ID sp, ID station, String host, int port, String name, int chosen);

    boolean removeStation(ID sp, ID station);

    boolean removeStations(ID sp);

    //
    //  APIs
    //

    class ApiInfo {
        public String upload;
        public String download;
        public String avatar;

        public ApiInfo(String upload, String download, String avatar) {
            this.upload = upload;
            this.download = download;
            this.avatar = avatar;
        }
    }

    ApiInfo getAPI(ID sp, ID station);
}
