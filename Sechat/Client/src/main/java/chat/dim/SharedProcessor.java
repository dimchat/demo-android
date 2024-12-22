/* license: https://mit-license.org
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

import java.util.List;

import chat.dim.cpu.ClientProcessorCreator;
import chat.dim.dkd.ContentProcessor;
import chat.dim.model.MessageDataSource;
import chat.dim.protocol.Bulletin;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.DocumentCommand;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.type.Duration;
import chat.dim.type.Pair;
import chat.dim.utils.FrequencyChecker;
import chat.dim.utils.Log;

public class SharedProcessor extends ClientMessageProcessor {

    // each query will be expired after 10 minutes
    public static Duration QUERY_EXPIRES = Duration.ofMinutes(10);

    private final FrequencyChecker<Pair<ID, ID>> groupQueries = new FrequencyChecker<>(QUERY_EXPIRES);

    public SharedProcessor(ClientFacebook facebook, ClientMessenger messenger) {
        super(facebook, messenger);
    }

    @Override
    protected ContentProcessor.Creator createCreator(Facebook facebook, Messenger messenger) {
        return new ClientProcessorCreator(facebook, messenger);
    }

    @Override
    public List<Content> processContent(Content content, ReliableMessage rMsg) {
        if (!(content instanceof Command)) {
            ID group = content.getGroup();
            if (group != null) {
                ID sender = rMsg.getSender();
                Pair<ID, ID> direction = new Pair<>(sender, group);
                Bulletin doc = getFacebook().getBulletin(group);
                if (doc == null && groupQueries.isExpired(direction, null, false)) {
                    Log.info("querying group: " + group + ", " + sender);
                    Command cmd1 = DocumentCommand.query(group);
                    getMessenger().sendContent(cmd1, null, sender, 1);
                    Command cmd2 = GroupCommand.query(group);
                    getMessenger().sendContent(cmd2, null, sender, 1);
                }
            }
        }
        return super.processContent(content, rMsg);
    }

    @Override
    public List<InstantMessage> processInstantMessage(InstantMessage iMsg, ReliableMessage rMsg) {
        List<InstantMessage> responses = super.processInstantMessage(iMsg, rMsg);
        // save instant message
        MessageDataSource mds = MessageDataSource.getInstance();
        if (!mds.saveInstantMessage(iMsg)) {
            // error
            return null;
        }
        return responses;
    }

}
