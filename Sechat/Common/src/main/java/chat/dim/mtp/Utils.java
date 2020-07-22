/* license: https://mit-license.org
 *
 *  MTP: Message Transfer Protocol
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
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
package chat.dim.mtp;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import chat.dim.ID;
import chat.dim.ReliableMessage;
import chat.dim.dmtp.protocol.Message;
import chat.dim.dmtp.values.BinaryValue;
import chat.dim.dmtp.values.StringValue;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.protocol.ContentType;
import chat.dim.tlv.Data;
import chat.dim.tlv.IntegerData;
import chat.dim.tlv.MutableData;
import chat.dim.tlv.VarIntData;

public class Utils {

    @SuppressWarnings("unchecked")
    public static byte[] serializeMessage(ReliableMessage msg) {
        Map<String, Object> info = new HashMap<>(msg);
        //
        //  envelope
        //
        Object sender = info.get("sender");
        if (sender instanceof ID) {
            info.put("sender", sender.toString());
        }
        Object receiver = info.get("receiver");
        if (receiver instanceof ID) {
            info.put("receiver", receiver.toString());
        }
        Object type = info.get("type");
        if (type instanceof ContentType) {
            info.put("type", ((ContentType) type).value);
        }
        Object group = info.get("group");
        if (group instanceof ID) {
            info.put("group", group.toString());
        }
        //
        //  body
        //
        String content = (String) info.get("data");
        if (content != null) {
            if (content.startsWith("{")) {
                // JsON
                //noinspection CharsetObjectCanBeUsed
                info.put("data", content.getBytes(Charset.forName("UTF-8")));
            } else {
                // Base64
                info.put("data", Base64.decode(content));
            }
        }
        String signature = (String) info.get("signature");
        if (signature != null) {
            info.put("signature", Base64.decode(signature));
        }
        // symmetric key, keys
        String key = (String) info.get("key");
        if (key == null) {
            Map<Object, String> keys = (Map<Object, String>) info.get("keys");
            if (keys != null) {
                Data data = buildKeys(keys);
                data = KEYS_PREFIX.concat(data);
                // DMTP store both 'keys' and 'key' in 'key'
                info.put("key", data.getBytes());
            }
        } else {
            info.put("key", Base64.decode(key));
        }
        //
        //  attachments
        //
        Map<String, Object> meta = (Map<String, Object>) info.get("meta");
        if (meta != null) {
            // dict to JSON
            info.put("meta", JSON.encode(meta));
        }
        Map<String, Object> profile = (Map<String, Object>) info.get("profile");
        if (profile != null) {
            // dict to JSON
            info.put("profile", JSON.encode(profile));
        }

        // create as message
        Message message = Message.create(info);
        return message.getBytes();
    }

    public static ReliableMessage deserializeMessage(byte[] data) {
        Message msg = Message.parse(new Data(data));
        if (msg == null || msg.getSender() == null || msg.getReceiver() == null) {
            throw new NullPointerException("failed to deserialize data: " + Arrays.toString(data));
        }
        Map<String, Object> info = new HashMap<>();
        //
        //  envelope
        //
        info.put("sender", msg.getSender());
        info.put("receiver", msg.getReceiver());
        info.put("time", msg.getTimestamp());
        int type = msg.getType();
        if (type > 0) {
            info.put("type", type);
        }
        String group = msg.getGroup();
        if (group != null) {
            info.put("group", group);
        }
        //
        //  body
        //
        Data content = msg.getContent();
        if (content != null) {
            if (content.getByte(0) == '{') {
                // JsON
                info.put("data", content.toString());
            } else {
                // Base64
                info.put("data", Base64.encode(content.getBytes()));
            }
        }
        Data signature = msg.getSignature();
        if (signature != null) {
            info.put("signature", Base64.encode(signature.getBytes()));
        }
        // symmetric key, keys
        Data key = msg.getKey();
        if (key != null && key.getLength() > 5) {
            if (key.slice(0, 5).equals(KEYS_PREFIX)) {
                // 'KEYS:'
                info.put("keys", parseKeys(key.slice(5)));
            } else {
                info.put("key", Base64.encode(key.getBytes()));
            }
        }
        //
        //  attachments
        //
        Data meta = msg.getMeta();
        if (meta != null && meta.getLength() > 0) {
            // JsON to dict
            info.put("meta", JSON.decode(meta.getBytes()));
        }
        Data profile = msg.getProfile();
        if (profile != null && profile.getLength() > 0) {
            // JsON to dict
            info.put("profile", JSON.decode(profile.getBytes()));
        }

        // create reliable message
        return ReliableMessage.getInstance(info);
    }

    private static Data KEYS_PREFIX = new Data("KEYS:".getBytes());

    private static Map<String, Object> parseKeys(Data data) {
        Map<String, Object> keys = new HashMap<>();
        IntegerData size;
        StringValue name;
        BinaryValue value;
        while (data.getLength() > 0) {
            // get key length
            size = VarIntData.fromData(data);
            data = data.slice(size.getLength());
            // get key name
            name = new StringValue(data.slice(0, size.getIntValue()));
            data = data.slice(size.getIntValue());
            // get value length
            size = VarIntData.fromData(data);
            data = data.slice(size.getLength());
            // get value
            value = new BinaryValue(data.slice(0, size.getIntValue()));
            data = data.slice(size.getIntValue());
            if (name.getLength() > 0 && value.getLength() > 0) {
                keys.put(name.string, Base64.encode(value.getBytes()));
            }
        }
        return keys;
    }

    private static Data buildKeys(Map<Object, String> keys) {
        MutableData data = new MutableData(512);
        Object key;
        String base64;
        StringValue idValue;
        BinaryValue keyValue;
        for (Map.Entry<Object, String> entry : keys.entrySet()) {
            key = entry.getKey();
            if (key instanceof ID) {
                idValue = new StringValue(key.toString());
            } else if (key instanceof String) {
                idValue = new StringValue((String) key);
            } else {
                assert key == null : "error key: " + key;
                continue;
            }
            base64 = entry.getValue();
            if (idValue.getLength() > 0 && base64 != null && base64.length() > 0) {
                keyValue = new BinaryValue(Base64.decode(base64));
                data.append(new VarIntData(idValue.getLength()));
                data.append(idValue);
                data.append(new VarIntData(keyValue.getLength()));
                data.append(keyValue);
            }
        }
        return data;
    }
}
