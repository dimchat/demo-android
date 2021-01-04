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

import chat.dim.User;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.HandshakeCommandProcessor;
import chat.dim.cpu.LoginCommandProcessor;
import chat.dim.cpu.SearchCommandProcessor;
import chat.dim.cpu.StorageCommandProcessor;
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
import chat.dim.stargate.StarShip;
import chat.dim.utils.Log;

public class MessageProcessor extends chat.dim.common.MessageProcessor {

    public MessageProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
    public Content process(Content content, ReliableMessage rMsg) {
        Content res = super.process(content, rMsg);
        if (res == null) {
            // respond nothing
            return null;
        }
        if (res instanceof HandshakeCommand) {
            // urgent command
            return res;
        }

        ID sender = rMsg.getSender();
        if (res instanceof ReceiptCommand) {
            if (NetworkType.STATION.equals(sender.getType())) {
                // no need to respond receipt to station
                return null;
            }
            Log.info("receipt to sender: " + sender);
        }

        // check receiver
        ID receiver = rMsg.getReceiver();
        User user = getFacebook().selectLocalUser(receiver);
        assert user != null : "receiver error: " + receiver;
        // pack message
        Envelope env = Envelope.create(user.identifier, sender, null);
        InstantMessage iMsg = InstantMessage.create(env, res);
        // normal response
        getMessenger().sendMessage(iMsg, null, StarShip.SLOWER);
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
