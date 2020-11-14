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
package chat.dim.common;

import java.util.Map;

import chat.dim.CommandParser;
import chat.dim.Entity;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;

public class MessageProcessor extends chat.dim.MessageProcessor {

    public MessageProcessor(Messenger messenger) {
        super(messenger);
    }

    protected Messenger getMessenger() {
        return (Messenger) super.getMessenger();
    }

    protected Facebook getFacebook() {
        return (Facebook) super.getFacebook();
    }

    @Override
    protected Content process(Content content, ID sender, ReliableMessage rMsg) {
        if (getMessenger().checkGroup(content, sender)) {
            // save this message in a queue to wait group meta response
            ID group = content.getGroup();
            rMsg.put("waiting", group);
            getMessenger().suspendMessage(rMsg);
            return null;
        }
        try {
            return super.process(content, sender, rMsg);
        } catch (NullPointerException e) {
            e.printStackTrace();
            String text = e.getMessage();
            if (text.contains("failed to get meta for ")) {
                int pos = text.indexOf(": ");
                if (pos > 0) {
                    ID waiting = Entity.parseID(text.substring(pos + 2));
                    if (waiting == null) {
                        throw new NullPointerException("failed to get ID: " + text);
                    } else {
                        rMsg.put("waiting", waiting);
                        getMessenger().suspendMessage(rMsg);
                    }
                }
            }
            return null;
        }
    }

    static {
        // replace command parser
        Command.parser = new CommandParser() {

            @Override
            protected Command parseCommand(Map<String, Object> cmd, String name) {
                // parse core command first
                Command core = super.parseCommand(cmd, name);
                if (core != null) {
                    return core;
                }

                // search command
                if (SearchCommand.SEARCH.equals(name)) {
                    return new SearchCommand(cmd);
                }
                if (SearchCommand.ONLINE_USERS.equals(name)) {
                    return new SearchCommand(cmd);
                }

                // report command
                if (ReportCommand.REPORT.equals(name)) {
                    return new ReportCommand(cmd);
                }
                if (ReportCommand.ONLINE.equals(name)) {
                    return new ReportCommand(cmd);
                }
                if (ReportCommand.OFFLINE.equals(name)) {
                    return new ReportCommand(cmd);
                }

                return null;
            }
        };
    }
}
