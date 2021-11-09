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

import chat.dim.Messenger;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.MuteCommand;

public class CommonProcessorFactory extends ProcessorFactory {

    public CommonProcessorFactory(Messenger messenger) {
        super(messenger);
    }

    @Override
    protected ContentProcessor createProcessor(int type) {
        // file
        if (ContentType.FILE.equals(type)) {
            return new FileContentProcessor((chat.dim.common.Messenger) getMessenger());
        } else if (ContentType.IMAGE.equals(type) || ContentType.AUDIO.equals(type) || ContentType.VIDEO.equals(type)) {
            ContentProcessor cpu = contentProcessors.get(ContentType.FILE.value);
            if (cpu == null) {
                cpu = new FileContentProcessor((chat.dim.common.Messenger) getMessenger());
                contentProcessors.put(ContentType.FILE.value, cpu);
            }
            return cpu;
        }
        ContentProcessor cpu = super.createProcessor(type);
        if (cpu == null) {
            // unknown
            return new AnyContentProcessor(getMessenger());
        }
        return cpu;
    }

    @Override
    protected CommandProcessor createProcessor(int type, String command) {
        // receipt
        if (Command.RECEIPT.equals(command)) {
            return new ReceiptCommandProcessor(getMessenger());
        }
        // mute
        if (MuteCommand.MUTE.equals(command)) {
            return new MuteCommandProcessor(getMessenger());
        }
        // block
        if (BlockCommand.BLOCK.equals(command)) {
            return new BlockCommandProcessor(getMessenger());
        }
        // others
        return super.createProcessor(type, command);
    }
}
