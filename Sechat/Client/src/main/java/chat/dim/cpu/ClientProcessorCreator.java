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

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.protocol.AnsCommand;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;

public class ClientProcessorCreator extends ClientContentProcessorCreator {

    public ClientProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public ContentProcessor createContentProcessor(int type) {
        // application customized
        if (ContentType.APPLICATION.equals(type)) {
            return new AppContentProcessor(getFacebook(), getMessenger());
        } else if (ContentType.CUSTOMIZED.equals(type)) {
            return new AppContentProcessor(getFacebook(), getMessenger());
        }
        // default
        if (0 == type) {
            return new AnyContentProcessor(getFacebook(), getMessenger());
        }
        return super.createContentProcessor(type);
    }

    @Override
    public ContentProcessor createCommandProcessor(int type, String name) {
        // storage (contacts, private_key)
        if (StorageCommand.STORAGE.equals(name)) {
            return new StorageCommandProcessor(getFacebook(), getMessenger());
        } else if (StorageCommand.CONTACTS.equals(name) || StorageCommand.PRIVATE_KEY.equals(name)) {
            return new StorageCommandProcessor(getFacebook(), getMessenger());
        }
        // search
        if (SearchCommand.SEARCH.equals(name)) {
            return new SearchCommandProcessor(getFacebook(), getMessenger());
        } else if (SearchCommand.ONLINE_USERS.equals(name)) {
            return new SearchCommandProcessor(getFacebook(), getMessenger());
        }
        // mute
        if (MuteCommand.MUTE.equals(name)) {
            return new MuteCommandProcessor(getFacebook(), getMessenger());
        }
        // block
        if (BlockCommand.BLOCK.equals(name)) {
            return new BlockCommandProcessor(getFacebook(), getMessenger());
        }
        // ans
        if (AnsCommand.ANS.equals(name)) {
            return new AnsCommandProcessor(getFacebook(), getMessenger());
        }
        // others
        return super.createCommandProcessor(type, name);
    }
}
