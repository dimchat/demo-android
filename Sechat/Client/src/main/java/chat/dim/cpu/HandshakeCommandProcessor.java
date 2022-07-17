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

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.network.Server;
import chat.dim.protocol.Content;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.utils.Log;

public class HandshakeCommandProcessor extends BaseCommandProcessor {

    public HandshakeCommandProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public List<Content> process(Content content, ReliableMessage rMsg) {
        assert content instanceof HandshakeCommand : "handshake command error: " + content;
        HandshakeCommand cmd = (HandshakeCommand) content;
        String message = cmd.getMessage();
        String sessionKey = cmd.getSessionKey();
        ID sender = rMsg.getSender();
        Log.info("received 'handshake': " + sender + ", " + message + ", " + sessionKey);
        Messenger messenger = (Messenger) getMessenger();
        Server server = messenger.getCurrentServer();
        if (!server.getIdentifier().equals(sender)) {
            Log.error("!!! ignore error handshake from this sender: " + sender + ", " + server);
            return null;
        }
        if ("DIM!".equals(message)) {
            // S -> C
            Log.info("handshake success!");
            server.handshakeAccepted();
            return null;
        } else if ("DIM?".equals(message)) {
            // S -> C
            Log.info("handshake again, session key: " + sessionKey);
            server.handshake(sessionKey);
            return null;
        } else {
            // C -> S: Hello world!
            throw new IllegalStateException("handshake command error: " + cmd);
        }
    }
}
