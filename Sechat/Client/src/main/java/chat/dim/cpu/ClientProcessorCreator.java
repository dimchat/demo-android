/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
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

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.protocol.Command;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;

public class ClientProcessorCreator extends CommonProcessorCreator {

    public ClientProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    protected Facebook getFacebook() {
        return (Facebook) super.getFacebook();
    }

    @Override
    protected Messenger getMessenger() {
        return (Messenger) super.getMessenger();
    }

    @Override
    public ContentProcessor createProcessor(int type, String command) {
        // handshake, login
        if (Command.HANDSHAKE.equals(command)) {
            return new HandshakeCommandProcessor(getFacebook(), getMessenger());
        } else if (Command.LOGIN.equals(command)) {
            return new LoginCommandProcessor(getFacebook(), getMessenger());
        }
        // storage (contacts, private_key)
        if (StorageCommand.STORAGE.equals(command)) {
            return new StorageCommandProcessor(getFacebook(), getMessenger());
        } else if (StorageCommand.CONTACTS.equals(command) || StorageCommand.PRIVATE_KEY.equals(command)) {
            return new StorageCommandProcessor(getFacebook(), getMessenger());
        }
        // search
        if (SearchCommand.SEARCH.equals(command)) {
            return new SearchCommandProcessor(getFacebook(), getMessenger());
        } else if (SearchCommand.ONLINE_USERS.equals(command)) {
            return new SearchCommandProcessor(getFacebook(), getMessenger());
        }
        // others
        return super.createProcessor(type, command);
    }
}
