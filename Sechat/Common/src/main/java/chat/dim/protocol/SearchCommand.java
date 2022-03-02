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
package chat.dim.protocol;

import java.util.List;
import java.util.Map;

import chat.dim.dkd.BaseCommand;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      command  : "search",        // or "users"
 *
 *      keywords : "keywords",      // keyword string
 *      users    : ["ID"],          // user ID list
 *      results  : {"ID": {meta}, } // user's meta map
 *  }
 */
public class SearchCommand extends BaseCommand {

    public static final String SEARCH = "search";

    // search online users
    public static final String ONLINE_USERS = "users";

    public SearchCommand(Map<String, Object> dictionary) {
        super(dictionary);
    }

    public SearchCommand(String keywords) {
        super(ONLINE_USERS.equals(keywords) ? ONLINE_USERS : SEARCH);

        if (!ONLINE_USERS.equals(keywords)) {
            put("keywords", keywords);
        }
    }

    /**
     *  Get user ID list
     *
     * @return ID string list
     */
    @SuppressWarnings("unchecked")
    public List<ID> getUsers() {
        List<String> users = (List) get("users");
        if (users == null) {
            return null;
        }
        return ID.convert(users);
    }

    /**
     *  Get user metas mapping to ID strings
     *
     * @return meta dictionary
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getResults() {
        Map<String, Object> results = (Map) get("results");
        if (results == null) {
            return null;
        }
        return results;
    }
}
