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
package chat.dim.common;

import java.util.ArrayList;
import java.util.List;

import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Command;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;

public abstract class Messenger extends chat.dim.Messenger {

    public Messenger()  {
        super();
        setCipherKeyDelegate(KeyStore.getInstance());
    }

    @Override
    public Facebook getFacebook() {
        return (Facebook) super.getFacebook();
    }

    @Override
    protected MessagePacker getMessagePacker() {
        return (MessagePacker) super.getMessagePacker();
    }
    @Override
    protected MessagePacker createMessagePacker() {
        return new MessagePacker(this);
    }

    @Override
    protected MessageProcessor getMessageProcessor() {
        return (MessageProcessor) super.getMessageProcessor();
    }
    @Override
    protected MessageProcessor createMessageProcessor() {
        return new MessageProcessor(this);
    }

    @Override
    protected MessageTransmitter getMessageTransmitter() {
        return (MessageTransmitter) super.getMessageTransmitter();
    }
    @Override
    protected MessageTransmitter createMessageTransmitter() {
        return new MessageTransmitter(this);
    }

    @Override
    public byte[] serializeKey(SymmetricKey password, InstantMessage iMsg) {
        Object reused = password.get("reused");
        if (reused != null) {
            ID receiver = iMsg.getReceiver();
            if (ID.isGroup(receiver)) {
                // reuse key for grouped message
                return null;
            }
            // remove before serialize key
            password.remove("reused");
        }
        byte[] data = super.serializeKey(password, iMsg);
        if (reused != null) {
            // put it back
            password.put("reused", reused);
        }
        return data;
    }

    //
    //  Interfaces for Sending Commands
    //

    public abstract boolean queryMeta(ID identifier);

    public abstract boolean queryProfile(ID identifier);

    public abstract boolean queryGroupInfo(ID group, List<ID> members);

    public boolean queryGroupInfo(ID group, ID member) {
        List<ID> array = new ArrayList<>();
        array.add(member);
        return queryGroupInfo(group, array);
    }

    public abstract boolean sendCommand(Command cmd, int priority);
}
