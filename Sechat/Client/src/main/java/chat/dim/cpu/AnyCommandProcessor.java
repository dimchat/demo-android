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
package chat.dim.cpu;

import chat.dim.Messenger;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;

public class AnyCommandProcessor extends CommandProcessor {

    public AnyCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
    protected CommandProcessor newCommandProcessor(String command) {
        //
        //  Common Commands
        //
        if (Command.RECEIPT.equalsIgnoreCase(command)) {
            return new ReceiptCommandProcessor(getMessenger());
        }

        if (MuteCommand.MUTE.equalsIgnoreCase(command)) {
            return new MuteCommandProcessor(getMessenger());
        }
        if (BlockCommand.BLOCK.equalsIgnoreCase(command)) {
            return new BlockCommandProcessor(getMessenger());
        }

        //
        //  Client Commands
        //
        if (Command.HANDSHAKE.equalsIgnoreCase(command)) {
            return new HandshakeCommandProcessor(getMessenger());
        }
        if (Command.LOGIN.equalsIgnoreCase(command)) {
            return new LoginCommandProcessor(getMessenger());
        }

        // storage (contacts, private_key)
        if (StorageCommand.STORAGE.equalsIgnoreCase(command)) {
            return new StorageCommandProcessor(getMessenger());
        }
        if (StorageCommand.CONTACTS.equalsIgnoreCase(command)) {
            return new StorageCommandProcessor(getMessenger());
        }
        if (StorageCommand.PRIVATE_KEY.equalsIgnoreCase(command)) {
            return new StorageCommandProcessor(getMessenger());
        }

        if (SearchCommand.SEARCH.equalsIgnoreCase(command)) {
            return new SearchCommandProcessor(getMessenger());
        }
        if (SearchCommand.ONLINE_USERS.equalsIgnoreCase(command)) {
            return new SearchCommandProcessor(getMessenger());
        }

        return super.newCommandProcessor(command);
    }
}
