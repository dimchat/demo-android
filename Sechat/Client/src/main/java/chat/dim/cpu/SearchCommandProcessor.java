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
package chat.dim.cpu;

import java.util.Map;

import chat.dim.Content;
import chat.dim.Facebook;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Messenger;
import chat.dim.Meta;
import chat.dim.notification.NotificationCenter;
import chat.dim.protocol.SearchCommand;

public class SearchCommandProcessor extends CommandProcessor {

    public static final String SearchUpdated = "SearchUpdated";

    public SearchCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Meta getMeta(ID identifier, Object dictionary) {
        Meta meta;
        try {
            meta = Meta.getInstance(dictionary);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        if (meta.matches(identifier)) {
            return meta;
        }
        return null;
    }

    private void parse(SearchCommand cmd) {
        Map<String, Object> results = cmd.getResults();
        if (results == null) {
            return;
        }
        Facebook facebook = getFacebook();
        ID identifier;
        Meta meta;
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            identifier = facebook.getID(entry.getKey());
            meta = getMeta(identifier, entry.getValue());
            if (meta == null) {
                // TODO: meta error
                continue;
            }
            facebook.saveMeta(meta, identifier);
        }
    }

    @Override
    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof SearchCommand;

        parse((SearchCommand) content);

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(SearchUpdated, this, content);

        return null;
    }
}
