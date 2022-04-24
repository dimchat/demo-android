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

import com.alibaba.fastjson.JSONException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import chat.dim.CipherKeyDelegate;
import chat.dim.core.Packer;
import chat.dim.core.Processor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.FileContentProcessor;
import chat.dim.crypto.EncryptKey;
import chat.dim.crypto.SymmetricKey;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.ID;
import chat.dim.protocol.InstantMessage;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.SecureMessage;

public abstract class Messenger extends chat.dim.Messenger {

    private WeakReference<Delegate> delegateRef = null;

    private MessagePacker messagePacker = null;
    private MessageProcessor messageProcessor = null;

    public Messenger()  {
        super();
    }

    /**
     *  Delegate for Station
     *
     * @param delegate - message delegate
     */
    public void setDelegate(Delegate delegate) {
        delegateRef = new WeakReference<>(delegate);
    }
    public Delegate getDelegate() {
        return delegateRef == null ? null : delegateRef.get();
    }

    public abstract Facebook getFacebook();

    @Override
    public CipherKeyDelegate getCipherKeyDelegate() {
        return getKeyStore();
    }
    public KeyStore getKeyStore() {
        return KeyStore.getInstance();
    }

    /**
     *  Delegate for packing message
     */
    @Override
    protected Packer getPacker() {
        return getMessagePacker();
    }
    private MessagePacker getMessagePacker() {
        if (messagePacker == null) {
            messagePacker = createMessagePacker();
        }
        return messagePacker;
    }
    protected MessagePacker createMessagePacker() {
        return new MessagePacker(getFacebook(), this);
    }

    /**
     *  Delegate for processing message
     */
    @Override
    protected Processor getProcessor() {
        return getMessageProcessor();
    }
    private MessageProcessor getMessageProcessor() {
        if (messageProcessor == null) {
            messageProcessor = createMessageProcessor();
        }
        return messageProcessor;
    }
    protected MessageProcessor createMessageProcessor() {
        return new MessageProcessor(getFacebook(), this);
    }

    private FileContentProcessor getFileContentProcessor() {
        ContentProcessor cpu = getMessageProcessor().getProcessor(ContentType.FILE);
        assert cpu instanceof FileContentProcessor : "failed to get file content processor";
        return (FileContentProcessor) cpu;
    }

    @Override
    public byte[] serializeContent(Content content, SymmetricKey password, InstantMessage iMsg) {
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContentProcessor fpu = getFileContentProcessor();
            fpu.uploadFileContent((FileContent) content, password, iMsg);
        }
        return super.serializeContent(content, password, iMsg);
    }

    @Override
    public Content deserializeContent(byte[] data, SymmetricKey password, SecureMessage sMsg) {
        Content content;
        try {
            content = super.deserializeContent(data, password, sMsg);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        if (content == null) {
            throw new NullPointerException("failed to deserialize message content: " + sMsg);
        }
        // check attachment for File/Image/Audio/Video message content
        if (content instanceof FileContent) {
            FileContentProcessor fpu = getFileContentProcessor();
            fpu.downloadFileContent((FileContent) content, password, sMsg);
        }
        return content;
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

    @Override
    public byte[] encryptKey(byte[] data, ID receiver, InstantMessage iMsg) {
        EncryptKey key = getFacebook().getPublicKeyForEncryption(receiver);
        if (key == null) {
            // save this message in a queue waiting receiver's meta/document response
            suspendMessage(iMsg);
            //throw new NullPointerException("failed to get encrypt key for receiver: " + receiver);
            return null;
        }
        return super.encryptKey(data, receiver, iMsg);
    }

    //
    //  Interfaces for Message Storage
    //

    /**
     *  Suspend the received message for the sender's meta
     *
     * @param rMsg - message received from network
     */
    public abstract void suspendMessage(ReliableMessage rMsg);

    /**
     *  Suspend the sending message for the receiver's meta & visa,
     *  or group meta when received new message
     *
     * @param iMsg - instant message to be sent
     */
    public abstract void suspendMessage(InstantMessage iMsg);

    /**
     * Save the message into local storage
     *
     * @param iMsg - instant message
     * @return true on success
     */
    public abstract boolean saveMessage(InstantMessage iMsg);

    //
    //  Interfaces for Sending Commands
    //
    public abstract boolean sendContent(ID sender, ID receiver, Content content, int priority);

    public abstract boolean queryMeta(ID identifier);

    public abstract boolean queryDocument(ID identifier, String type);

    public abstract boolean queryGroupInfo(ID group, List<ID> members);

    public boolean queryGroupInfo(ID group, ID member) {
        List<ID> array = new ArrayList<>();
        array.add(member);
        return queryGroupInfo(group, array);
    }

    //
    //  Events
    //

    public void onConnected() {

    }

    //
    //  Interfaces for Station
    //
    public String uploadData(byte[] data, InstantMessage iMsg) {
        return getDelegate().uploadData(data, iMsg);
    }

    public byte[] downloadData(String url, InstantMessage iMsg) {
        return getDelegate().downloadData(url, iMsg);
    }

    /**
     *  Messenger Delegate
     *  ~~~~~~~~~~~~~~~~~~
     */
    public interface Delegate {

        /**
         *  Upload encrypted data to CDN
         *
         * @param data - encrypted file data
         * @param iMsg - instant message
         * @return download URL
         */
        String uploadData(byte[] data, InstantMessage iMsg);

        /**
         *  Download encrypted data from CDN
         *
         * @param url - download URL
         * @param iMsg - instant message
         * @return encrypted file data
         */
        byte[] downloadData(String url, InstantMessage iMsg);
    }

    static {
        // load factories from SDK
        registerAllFactories();

        // register command parsers
        Command.setFactory(SearchCommand.SEARCH, SearchCommand::new);
        Command.setFactory(SearchCommand.ONLINE_USERS, SearchCommand::new);

        Command.setFactory(ReportCommand.REPORT, ReportCommand::new);
        Command.setFactory(ReportCommand.ONLINE, ReportCommand::new);
        Command.setFactory(ReportCommand.OFFLINE, ReportCommand::new);
    }
}
