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

import chat.dim.ID;
import chat.dim.ReliableMessage;
import chat.dim.Messenger;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Content;
import chat.dim.protocol.MuteCommand;

public class MuteCommandProcessor extends CommandProcessor {

    public MuteCommandProcessor(Messenger messenger) {
        super(messenger);
    }

    private Content getMuteList() {
        // TODO: load mute-list from database
        return null;
    }

    private Content putMuteList(List list) {
        // TODO: save mute-list into database
        return null;
    }

    @Override
    public Content process(Content content, ID sender, ReliableMessage<ID, SymmetricKey> rMsg) {
        assert content instanceof MuteCommand : "mute command error: " + content;
        MuteCommand cmd = (MuteCommand) content;
        List list = cmd.getMuteList();
        if (list == null) {
            return getMuteList();
        } else {
            return putMuteList(list);
        }
    }
}
