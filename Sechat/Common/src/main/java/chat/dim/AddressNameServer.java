/* license: https://mit-license.org
 *
 *  DIM-SDK : Decentralized Instant Messaging Software Development Kit
 *
 *                                Written in 2022 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.ID;

public abstract class AddressNameServer implements AddressNameService {

    private final Map<String, Boolean> reserved = new HashMap<>();
    private final Map<String, ID> caches = new HashMap<>();
    private final Map<ID, List<String>> namesTables = new HashMap<>();

    protected AddressNameServer() {
        super();
        // constant ANS records
        caches.put("all", ID.EVERYONE);
        caches.put("everyone", ID.EVERYONE);
        caches.put("anyone", ID.ANYONE);
        caches.put("owner", ID.ANYONE);
        caches.put("founder", ID.FOUNDER);
        // reserved names
        for (String item : KEYWORDS) {
            reserved.put(item, true);
        }
    }

    @Override
    public boolean isReserved(String name) {
        Boolean value = reserved.get(name);
        if (value == null) {
            return false;
        }
        return value;
    }

    protected boolean cache(String name, ID identifier) {
        if (isReserved(name)) {
            // this name is reserved, cannot register
            return false;
        }
        if (identifier == null) {
            caches.remove(name);
            // TODO: only remove one table?
            namesTables.clear();
        } else {
            caches.put(name, identifier);
            // names changed, remove the table of names for this ID
            namesTables.remove(identifier);
        }
        return true;
    }

    @Override
    public ID identifier(String name) {
        return caches.get(name);
    }

    @Override
    public List<String> names(ID identifier) {
        List<String> array = namesTables.get(identifier);
        if (array == null) {
            array = new ArrayList<>();
            // TODO: update all tables?
            for (Map.Entry<String, ID> entry : caches.entrySet()) {
                if (identifier.equals(entry.getValue())) {
                    array.add(entry.getKey());
                }
            }
            namesTables.put(identifier, array);
        }
        return array;
    }

    public abstract boolean save(String name, ID identifier);
}
