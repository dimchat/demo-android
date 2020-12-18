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
package chat.dim.common;

import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.SecureMessage;
import chat.dim.threading.BackgroundThreads;

public class MessageTransmitter extends chat.dim.MessageTransmitter {

    public MessageTransmitter(Facebook facebook, Messenger messenger, MessagePacker packer) {
        super(facebook, messenger, packer);
    }

    @Override
    public boolean sendMessage(final InstantMessage iMsg, final Messenger.Callback callback, final int priority) {
        BackgroundThreads.wait(new Runnable() {
            @Override
            public void run() {
                // Send message (secured + certified) to target station
                SecureMessage sMsg = getPacker().encryptMessage(iMsg);
                if (sMsg == null) {
                    // public key not found?
                    return ;
                    //throw new NullPointerException("failed to encrypt message: " + iMsg);
                }
                ReliableMessage rMsg = getPacker().signMessage(sMsg);
                if (rMsg == null) {
                    // TODO: set iMsg.state = error
                    throw new NullPointerException("failed to sign message: " + sMsg);
                }

                sendMessage(rMsg, callback, priority);
                // TODO: if OK, set iMsg.state = sending; else set iMsg.state = waiting

                // save signature for receipt
                iMsg.put("signature", rMsg.get("signature"));

                getMessenger().saveMessage(iMsg);
            }
        });
        return true;
    }
}
