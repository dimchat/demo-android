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
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.MuteCommand;

public class CommonProcessorCreator extends ContentProcessorCreator {

    public CommonProcessorCreator(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
    }

    @Override
    public ContentProcessor createContentProcessor(int type) {
        // file
        if (ContentType.FILE.equals(type)) {
            return new FileContentProcessor(getFacebook(), getMessenger());
        } else if (ContentType.IMAGE.equals(type) || ContentType.AUDIO.equals(type) || ContentType.VIDEO.equals(type)) {
            return new FileContentProcessor(getFacebook(), getMessenger());
        }
        // others
        ContentProcessor cpu = super.createContentProcessor(type);
        if (cpu == null) {
            // unknown type?
            cpu = new AnyContentProcessor(getFacebook(), getMessenger());
        }
        return cpu;
    }

    @Override
    public ContentProcessor createCommandProcessor(int type, String command) {
        // receipt
        if (Command.RECEIPT.equals(command)) {
            return new ReceiptCommandProcessor(getFacebook(), getMessenger());
        }
        // mute
        if (MuteCommand.MUTE.equals(command)) {
            return new MuteCommandProcessor(getFacebook(), getMessenger());
        }
        // block
        if (BlockCommand.BLOCK.equals(command)) {
            return new BlockCommandProcessor(getFacebook(), getMessenger());
        }
        // others
        return super.createCommandProcessor(type, command);
    }
}
