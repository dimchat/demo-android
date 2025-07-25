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
import chat.dim.cpu.customized.AppContentHandler;
import chat.dim.cpu.customized.DriftBottleHandler;
import chat.dim.dkd.ContentProcessor;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.StorageCommand;

public class ClientProcessorCreator extends ClientContentProcessorCreator {

    public ClientProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    protected AppCustomizedProcessor createCustomizedContentProcessor(Facebook facebook, Messenger messenger) {
        AppCustomizedProcessor cpu = super.createCustomizedContentProcessor(facebook, messenger);
        // 'chat.dim.sechat:drift_bottle'
        cpu.setHandler(
                AppContentHandler.APP_ID,
                DriftBottleHandler.MOD_NAME,
                new DriftBottleHandler(facebook, messenger)
        );
        return cpu;
    }

    @Override
    public ContentProcessor createContentProcessor(String type) {
        switch (type) {
            // default
            case ContentType.ANY:
            case "*":
                return new AnyContentProcessor(getFacebook(), getMessenger());
        }
        // others
        return super.createContentProcessor(type);
    }

    @Override
    public ContentProcessor createCommandProcessor(String type, String name) {
        switch (name) {
            // storage (contacts, private_key)
            case StorageCommand.STORAGE:
            case StorageCommand.CONTACTS:
            case StorageCommand.PRIVATE_KEY:
                return new StorageCommandProcessor(getFacebook(), getMessenger());
            // search (users)
            case SearchCommand.SEARCH:
            case SearchCommand.ONLINE_USERS:
                return new SearchCommandProcessor(getFacebook(), getMessenger());
        }
        // others
        return super.createCommandProcessor(type, name);
    }
}
