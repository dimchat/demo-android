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

import chat.dim.client.Messenger;
import chat.dim.network.Server;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.HandshakeCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.utils.Log;

public class HandshakeCommandProcessor extends CommandProcessor {

    public HandshakeCommandProcessor() {
        super();
    }

    private Content success() {
        Log.info("handshake success!");
        Messenger messenger = (Messenger) getMessenger();
        Server server = messenger.getCurrentServer();
        server.handshakeAccepted();
        return null;
    }

    private Content restart(String sessionKey) {
        Log.info("handshake again, session key: " + sessionKey);
        Messenger messenger = (Messenger) getMessenger();
        Server server = messenger.getCurrentServer();
        server.handshake(sessionKey);
        return null;
    }

    @Override
    public Content execute(Command cmd, ReliableMessage rMsg) {
        assert cmd instanceof HandshakeCommand : "handshake command error: " + cmd;
        HandshakeCommand hCmd = (HandshakeCommand) cmd;
        String message = hCmd.message;
        if ("DIM!".equals(message)) {
            // S -> C
            return success();
        } else if ("DIM?".equals(message)) {
            // S -> C
            return restart(hCmd.sessionKey);
        } else {
            // C -> S: Hello world!
            throw new IllegalStateException("handshake command error: " + cmd);
        }
    }
}
