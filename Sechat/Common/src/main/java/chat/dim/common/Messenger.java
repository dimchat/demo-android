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

import chat.dim.core.CipherKeyDelegate;
import chat.dim.cpu.AnyContentProcessor;
import chat.dim.cpu.BlockCommandProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.MuteCommandProcessor;
import chat.dim.cpu.ReceiptCommandProcessor;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;

public abstract class Messenger extends chat.dim.Messenger {

    public Messenger()  {
        super();
    }

    @Override
    public Facebook getFacebook() {
        return (Facebook) super.getFacebook();
    }
    @Override
    protected Facebook createFacebook() {
        return new Facebook();
    }

    @Override
    public CipherKeyDelegate getCipherKeyDelegate() {
        CipherKeyDelegate keyCache = super.getCipherKeyDelegate();
        if (keyCache == null) {
            keyCache = getKeyStore();
            setCipherKeyDelegate(keyCache);
        }
        return keyCache;
    }
    public KeyStore getKeyStore() {
        return KeyStore.getInstance();
    }

    @Override
    protected MessagePacker createMessagePacker() {
        return new MessagePacker(this);
    }

    @Override
    protected MessageProcessor createMessageProcessor() {
        return new MessageProcessor(this);
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
            if (receiver.isGroup()) {
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

    public abstract boolean queryDocument(ID identifier);

    public abstract boolean queryGroupInfo(ID group, List<ID> members);

    public boolean queryGroupInfo(ID group, ID member) {
        List<ID> array = new ArrayList<>();
        array.add(member);
        return queryGroupInfo(group, array);
    }

    static {
        // load factories & processors from SDK
        MessageProcessor.registerAllFactories();
        MessageProcessor.registerAllProcessors();

        // register command parsers
        Command.register(SearchCommand.SEARCH, SearchCommand::new);
        Command.register(SearchCommand.ONLINE_USERS, SearchCommand::new);

        Command.register(ReportCommand.REPORT, ReportCommand::new);
        Command.register(ReportCommand.ONLINE, ReportCommand::new);
        Command.register(ReportCommand.OFFLINE, ReportCommand::new);

        // register content processors
        ContentProcessor.register(0, new AnyContentProcessor());

        // register command processors
        CommandProcessor.register(Command.RECEIPT, new ReceiptCommandProcessor());
        CommandProcessor.register(MuteCommand.MUTE, new MuteCommandProcessor());
        CommandProcessor.register(BlockCommand.BLOCK, new BlockCommandProcessor());
    }
}
