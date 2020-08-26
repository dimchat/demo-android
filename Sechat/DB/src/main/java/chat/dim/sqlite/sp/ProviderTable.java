/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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
package chat.dim.sqlite.sp;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.ArrayList;
import java.util.List;

import chat.dim.ID;
import chat.dim.model.Configuration;
import chat.dim.sqlite.DataTable;
import chat.dim.sqlite.mkm.EntityDatabase;

public class ProviderTable extends DataTable implements chat.dim.database.ProviderTable {

    private ProviderTable() {
        super(ProviderDatabase.getInstance());
    }

    private static ProviderTable ourInstance;
    public static ProviderTable getInstance() {
        if (ourInstance == null) {
            ourInstance = new ProviderTable();
        }
        return ourInstance;
    }

    //
    //  chat.dim.database.ProviderTable
    //

    @Override
    public List<ProviderInfo> getProviders() {
        String[] columns = {"spid", "name", "url", "chosen"};
        try (Cursor cursor = query(ProviderDatabase.T_PROVIDER, columns, null, null, null, null, "chosen DESC")) {
            List<ProviderInfo> providers = new ArrayList<>();
            ID identifier;
            String name;
            String url;
            int chosen;
            while (cursor.moveToNext()) {
                identifier = EntityDatabase.getID(cursor.getString(0));
                name = cursor.getString(1);
                url = cursor.getString(2);
                chosen = cursor.getInt(3);
                providers.add(new ProviderInfo(identifier, name, url, chosen));
            }
            return providers;
        }
    }

    @Override
    public boolean addProvider(ID identifier, String name, String url, int chosen) {
        ContentValues values = new ContentValues();
        values.put("spid", identifier.toString());
        values.put("name", name);
        values.put("url", url);
        values.put("chosen", chosen);
        return insert(ProviderDatabase.T_PROVIDER, null, values) >= 0;
    }

    @Override
    public boolean updateProvider(ID identifier, String name, String url, int chosen) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("url", url);
        values.put("chosen", chosen);
        String[] whereArgs = {identifier.toString()};
        return update(ProviderDatabase.T_PROVIDER, values, "spid=?", whereArgs) > 0;
    }

    @Override
    public boolean removeProvider(ID identifier) {
        String[] whereArgs = {identifier.toString()};
        return delete(ProviderDatabase.T_PROVIDER, "spid=?", whereArgs) > 0;
    }

    @Override
    public List<StationInfo> getStations(ID sp) {
        String[] columns = {"sid", "name", "host", "port", "chosen"};
        String[] selectionArgs = {sp.toString()};
        try (Cursor cursor = query(ProviderDatabase.T_STATION, columns, "spid=?", selectionArgs, null, null, "chosen DESC")) {
            List<StationInfo> stations = new ArrayList<>();
            ID identifier;
            String name;
            String host;
            int port;
            int chosen;
            while (cursor.moveToNext()) {
                identifier = EntityDatabase.getID(cursor.getString(0));
                name = cursor.getString(1);
                host = cursor.getString(2);
                port = cursor.getInt(3);
                chosen = cursor.getInt(4);
                stations.add(new StationInfo(identifier, name, host, port, chosen));
            }
            return stations;
        }
    }

    @Override
    public boolean addStation(ID sp, ID station, String host, int port, String name, int chosen) {
        ContentValues values = new ContentValues();
        values.put("spid", sp.toString());
        values.put("sid", station.toString());
        values.put("name", name);
        values.put("host", host);
        values.put("port", port);
        values.put("chosen", chosen);
        return insert(ProviderDatabase.T_STATION, null, values) >= 0;
    }

    @Override
    public boolean updateStation(ID sp, ID station, String host, int port, String name, int chosen) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("host", host);
        values.put("port", port);
        values.put("chosen", chosen);
        String[] whereArgs = {sp.toString(), station.toString()};
        return update(ProviderDatabase.T_STATION, values, "spid=? AND sid=?", whereArgs) > 0;
    }

    @Override
    public boolean chooseStation(ID sp, ID station) {
        ContentValues values = new ContentValues();
        values.put("chosen", 0);
        String[] whereArgs1 = {sp.toString()};
        update(ProviderDatabase.T_STATION, values, "spid=? AND chosen=1", whereArgs1);

        values.put("chosen", 1);
        String[] whereArgs2 = {sp.toString(), station.toString()};
        return update(ProviderDatabase.T_STATION, values, "spid=? AND sid=?", whereArgs2) > 0;
    }

    @Override
    public boolean removeStation(ID sp, ID station) {
        String[] whereArgs = {sp.toString(), station.toString()};
        return delete(ProviderDatabase.T_STATION, "spid=? AND sid=?", whereArgs) > 0;
    }

    @Override
    public boolean removeStations(ID sp) {
        String[] whereArgs = {sp.toString()};
        return delete(ProviderDatabase.T_STATION, "spid=?", whereArgs) > 0;
    }

    @Override
    public ApiInfo getAPI(ID sp, ID station) {
        // TODO: save APIs into database
        Configuration config = Configuration.getInstance();
        String upload = config.getUploadURL();
        String download = config.getDownloadURL();
        String avatar = config.getAvatarURL();
        return new ApiInfo(upload, download, avatar);
    }
}
