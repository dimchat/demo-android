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
package chat.dim.client;

import java.util.List;

import chat.dim.User;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.cpu.SearchCommandProcessor;
import chat.dim.cpu.StorageCommandProcessor;
import chat.dim.port.Departure;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.Envelope;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.NetworkType;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;
import chat.dim.protocol.TextContent;
import chat.dim.utils.Log;

public class MessageProcessor extends chat.dim.common.MessageProcessor {

    public MessageProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
    protected Messenger getMessenger() {
        return (Messenger) super.getMessenger();
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        List<Content> responses = super.process(content, rMsg);
        if (responses == null || responses.size() == 0) {
            // respond nothing
            return null;
        } else if (responses.get(0) instanceof HandshakeCommand) {
            // urgent command
            return responses;
        }

        ID sender = rMsg.getSender();
        ID receiver = rMsg.getReceiver();
        User user = getFacebook().selectLocalUser(receiver);
        assert user != null : "receiver error: " + receiver;

        Envelope env;
        InstantMessage iMsg;
        Messenger messenger = getMessenger();

        // check responses
        for (Content res : responses) {
            if (res == null) {
                // should not happen
                continue;
            } else if (res instanceof ReceiptCommand) {
                if (NetworkType.STATION.equals(sender.getType())) {
                    // no need to respond receipt to station
                    continue;
                }
                Log.info("receipt to sender: " + sender);
            } else if (res instanceof TextContent) {
                if (NetworkType.STATION.equals(sender.getType())) {
                    // no need to respond text message to station
                    continue;
                }
                Log.info("text to sender: " + sender);
            }
            // pack message
            env = Envelope.create(user.identifier, sender, null);
            iMsg = InstantMessage.create(env, res);
            // normal response
            messenger.sendMessage(iMsg, null, Departure.Priority.SLOWER.value);
        }

        // DON'T respond to station directly
        return null;
    }

    static {
        // register command processors
        CommandProcessor.register(Command.HANDSHAKE, new HandshakeCommandProcessor());
        CommandProcessor.register(Command.LOGIN, new LoginCommandProcessor());

        // storage (contacts, private_key)
        StorageCommandProcessor storageProcessor = new StorageCommandProcessor();
        CommandProcessor.register(StorageCommand.STORAGE, storageProcessor);
        CommandProcessor.register(StorageCommand.CONTACTS, storageProcessor);
        CommandProcessor.register(StorageCommand.PRIVATE_KEY, storageProcessor);

        // search (online)
        SearchCommandProcessor searchProcessor = new SearchCommandProcessor();
        CommandProcessor.register(SearchCommand.SEARCH, searchProcessor);
        CommandProcessor.register(SearchCommand.ONLINE_USERS, searchProcessor);
    }
}
