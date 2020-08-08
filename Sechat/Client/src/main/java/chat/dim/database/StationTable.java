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

import java.io.IOException;
import java.util.List;
import java.util.Map;

import chat.dim.ID;

public class StationTable extends Database {

    // "/sdcard/chat.dim.sechat/dim/{SP_ADDRESS}/stations.js"

    private static String getStationsFilePath(ID sp) {
        return getProviderFilePath(sp, "stations.js");
    }

    public boolean saveStations(List<Map<String, Object>> stations, ID sp) {
        String path = getStationsFilePath(sp);
        try {
            return saveJSON(stations, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> allStations(ID sp) {
        String path = getStationsFilePath(sp);
        try {
            //noinspection unchecked
            return (List) loadJSON(path);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
