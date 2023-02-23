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

import java.util.List;
import java.util.Map;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.notification.NotificationCenter;
import chat.dim.notification.NotificationNames;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SearchCommand;

public class SearchCommandProcessor extends BaseCommandProcessor {

    public SearchCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @SuppressWarnings("unchecked")
    private void parse(SearchCommand cmd) {
        Map<String, Object> results = cmd.getResults();
        if (results == null) {
            return;
        }
        Facebook facebook = getFacebook();
        ID identifier;
        Meta meta;
        Map<String, Object> info;
        Object version;
        for (Map.Entry<String, Object> entry : results.entrySet()) {
            identifier = ID.parse(entry.getKey());
            if (identifier == null) {
                // TODO: ID error
                continue;
            }
            info = (Map<String, Object>) entry.getValue();
            version = info.get("version");
            if (version != null) {
                info.put("type", version);
            }
            meta = Meta.parse(info);
            if (meta == null || !Meta.matches(identifier, meta)) {
                // TODO: meta error
                continue;
            }
            facebook.saveMeta(meta, identifier);
        }
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof SearchCommand : "search command error: " + content;
        SearchCommand cmd = (SearchCommand) content;
        parse(cmd);

        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(NotificationNames.SearchUpdated, this, cmd);

        return null;
    }
}
