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

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Messenger;
import chat.dim.protocol.StorageCommand;

public class StorageCommandProcessor extends CommandProcessor {

    public StorageCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content processContacts(StorageCommand cmd) {
        assert cmd.get("ID") != null;
        // decrypt and save contacts for user
        return null;
    }

    private Content processPrivateKey(StorageCommand cmd) {
        assert cmd.get("ID") != null;
        // save private key for user
        return null;
    }

    @Override
    public Content process(Content content, ID sender, InstantMessage iMsg) {
        assert content instanceof StorageCommand;
        StorageCommand cmd = (StorageCommand) content;
        String title = cmd.title;
        if (title.equals(StorageCommand.CONTACTS)) {
            return processContacts(cmd);
        } else if (title.equals(StorageCommand.PRIVATE_KEY)) {
            return processPrivateKey(cmd);
        }
        throw new UnsupportedOperationException("Unsupported storage, title: " + title);
    }
}
