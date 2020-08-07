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
package chat.dim.database;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.crypto.SymmetricKey;
import chat.dim.model.Messenger;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.utils.Times;

public class MessageTable extends Database {

    private Map<ID, List<InstantMessage>> chatHistory = new HashMap<>();

    private List cacheMessages(Object array, ID entity) {
        if (!(array instanceof List)) {
            return null;
        }
        List list = (List) array;
        List<InstantMessage> messages = new ArrayList<>();
        for (Object msg : list) {
            messages.add(InstantMessage.getInstance(msg));
        }
        chatHistory.put(entity, messages);
        return messages;
    }

    private List loadMessages(ID entity) {
        String path = getMessageFilePath(entity);
        try {
            Object array = loadJSON(path);
            return cacheMessages(array, entity);
        } catch (IOException e) {
            //e.printStackTrace();
            return null;
        }
    }

    private boolean saveMessages(ID entity) {
        List<InstantMessage> messages = chatHistory.get(entity);
        if (messages == null) {
            return false;
        }
        String path = getMessageFilePath(entity);
        try {
            return saveJSON(messages, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isEqual(InstantMessage<ID, SymmetricKey> msg1, InstantMessage<ID, SymmetricKey> msg2) {
        // 1. check sn
        msg1.setDelegate(Messenger.getInstance());
        msg2.setDelegate(Messenger.getInstance());
        if (msg1.getContent().serialNumber != msg2.getContent().serialNumber) {
            return false;
        }
        // 2. check time
        Date time1 = msg1.envelope.getTime();
        Date time2 = msg2.envelope.getTime();
        if (time1 == null) {
            return time2 == null;
        } else if (time2 == null) {
            return false;
        }
        long t1 = time1.getTime();
        long t2 = time2.getTime();
        // NOTICE: timestamp in seconds
        return t1 / 1000 == t2 / 1000;
    }

    private boolean isMatch(InstantMessage iMsg, ReceiptCommand receipt) {
        //noinspection unchecked
        iMsg.setDelegate(Messenger.getInstance());

        // 1. check sender & receiver
        Object sender = receipt.get("sender");
        if (sender != null && !iMsg.envelope.getSender().equals(sender)) {
            return false;
        }
        Object receiver = receipt.get("receiver");
        if (receiver != null && !iMsg.envelope.getReceiver().equals(receiver)) {
            return false;
        }
        // 2. check signature
        String sig1 = (String) receipt.get("signature");
        String sig2 = (String) iMsg.get("signature");
        if (sig1 != null && sig2 != null) {
            sig1 = sig1.replaceAll("\n", "");
            sig2 = sig2.replaceAll("\n", "");
            int len1 = sig1.length();
            int len2 = sig2.length();
            if (len1 > len2) {
                return sig1.contains(sig2);
            } else if (len1 < len2) {
                return sig2.contains(sig1);
            } else {
                return sig1.equals(sig2);
            }
        }
        // 3. check sn
        return iMsg.getContent().serialNumber == receipt.serialNumber;
    }

    //-------- messages

    public List<InstantMessage> messagesInConversation(ID entity) {
        List<InstantMessage> msgList = chatHistory.get(entity);
        if (msgList == null) {
            msgList = new ArrayList<>();
            List messages = loadMessages(entity);
            if (messages != null) {
                InstantMessage msg;
                for (Object item : messages) {
                    msg = InstantMessage.getInstance(item);
                    if (msg == null) {
                        throw new NullPointerException("message error: " + item);
                    }
                    msgList.add(msg);
                }
            }
            chatHistory.put(entity, msgList);
        }
        return msgList;
    }

    public int numberOfMessages(ID entity) {
        List<InstantMessage> msgList = messagesInConversation(entity);
        return msgList.size();
    }

    public InstantMessage messageAtIndex(int index, ID entity) {
        List<InstantMessage> msgList = messagesInConversation(entity);
        InstantMessage iMsg = msgList.get(index);
        //noinspection unchecked
        iMsg.setDelegate(Messenger.getInstance());
        return iMsg;
    }

    public boolean insertMessage(InstantMessage iMsg, ID entity) {
        Date time = iMsg.envelope.getTime();
        if (time == null) {
            time = new Date();
        }

        List<InstantMessage> msgList = messagesInConversation(entity);
        InstantMessage item;
        int index = msgList.size() - 1;
        for (; index >= 0; --index) {
            item = msgList.get(index);
            if (isEqual(item, iMsg)) {
                // duplicated message
                return false;
            }
            if (item.envelope.getTime() == null || Times.compare(item.envelope.getTime(), time) <= 0) {
                break;
            }
        }
        msgList.add(index + 1, iMsg);
        return saveMessages(entity);
    }

    public boolean removeMessage(InstantMessage iMsg, ID entity) {
        List<InstantMessage> msgList = messagesInConversation(entity);
        msgList.remove(iMsg);
        return saveMessages(entity);
    }

    public boolean withdrawMessage(InstantMessage iMsg, ID entity) {
        // TODO: withdraw a message;
        return false;
    }

    public boolean saveReceipt(InstantMessage iMsg, ID entity) {
        // save receipt of instant message
        if (!(iMsg.getContent() instanceof ReceiptCommand)) {
            return false;
        }
        ReceiptCommand receipt = (ReceiptCommand) iMsg.getContent();
        Object receiver = receipt.get("receiver");
        if (receiver != null) {
            entity = ID.getInstance(receiver);
        }

        List<InstantMessage> msgList = messagesInConversation(entity);
        InstantMessage item;
        int index = msgList.size() - 1;
        for (; index >= 0; --index) {
            item = msgList.get(index);
            if (!isMatch(item, receipt)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            List<Object> traces = (List<Object>) item.get("traces");
            if (traces == null) {
                traces = new ArrayList<>();
                item.put("traces", traces);
            }
            // DISCUSS: what about the other fields 'sender', 'receiver', 'signature'
            //          in this receipt command?
            traces.add(iMsg.envelope.getSender());
        }
        return saveMessages(entity);
    }

    public boolean removeMessages(ID entity) {
        String path = getMessageFilePath(entity);
        try {
            if (delete(path)) {
                chatHistory.remove(entity);
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean clearMessages(ID entity) {
        List<InstantMessage> messages = messagesInConversation(entity);
        if (messages == null) {
            return false;
        }
        messages.clear();
        String path = getMessageFilePath(entity);
        try {
            return saveJSON(messages, path);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
}
