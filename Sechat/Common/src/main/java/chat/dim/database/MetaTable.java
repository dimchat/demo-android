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
import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.Meta;

public class MetaTable extends Database {

    // profile cache
    private Map<ID, Meta> metaTable = new HashMap<>();

    private boolean cache(Meta meta, ID identifier) {
        if (meta.matches(identifier)) {
            metaTable.put(identifier, meta);
            return true;
        }
        return false;
    }

    // "/sdcard/chat.dim.sechat/mkm/{XX}/{address}/meta.js"
    private static String getMetaFilePath(ID entity) throws IOException {
        return getEntityFilePath(entity, "meta.js");
    }

    @SuppressWarnings("unchecked")
    private Meta loadMeta(ID entity) {
        try {
            // load from JsON file
            String path = getMetaFilePath(entity);
            Object dict = loadJSON(path);
            return Meta.getInstance((Map<String, Object>) dict);
        } catch (IOException | ClassNotFoundException e) {
            //e.printStackTrace();
            return null;
        }
    }

    public boolean saveMeta(Meta meta, ID entity) {
        if (!cache(meta, entity)) {
            return false;
        }
        try {
            // save into JsON file
            String path = getMetaFilePath(entity);
            if (exists(path)) {
                return true;
            }
            return saveJSON(meta, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Meta getMeta(ID entity) {
        // 1. try from meta cache
        Meta meta = metaTable.get(entity);
        if (meta == null) {
            // 2. load from JsON file
            meta = loadMeta(entity);
            if (meta == null) {
                // TODO: 3. place an empty meta for cache
                return null;
            }
            // no need to verify meta from local storage
            metaTable.put(entity, meta);
        }
        return meta;
    }
}
