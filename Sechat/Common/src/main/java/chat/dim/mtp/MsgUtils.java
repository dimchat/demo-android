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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import chat.dim.dmtp.protocol.Message;
import chat.dim.format.Base64;
import chat.dim.format.JSON;
import chat.dim.format.UTF8;
import chat.dim.protocol.ContentType;
import chat.dim.protocol.ID;
import chat.dim.protocol.ReliableMessage;
import chat.dim.tlv.values.RawValue;
import chat.dim.tlv.values.StringValue;
import chat.dim.type.ByteArray;
import chat.dim.type.Converter;
import chat.dim.type.Data;
import chat.dim.type.Dictionary;
import chat.dim.type.IntegerData;
import chat.dim.type.MutableData;
import chat.dim.type.VarIntData;

public class MsgUtils {

    @SuppressWarnings("unchecked")
    public static byte[] serializeMessage(ReliableMessage msg) {
        Dictionary dict = (Dictionary) msg;
        Map<String, Object> info = dict.copyMap(false);
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
        if (type instanceof String) {
            info.put("type", Converter.getInteger(type, 0));
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
                info.put("data", UTF8.encode(content));
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
            Map<Object, String> keys = (Map) info.get("keys");
            if (keys != null) {
                ByteArray data = buildKeys(keys);
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

        Map<String, Object> meta = (Map) info.get("meta");
        if (meta != null) {
            // dict to JSON
            info.put("meta", JSON.encode(meta));
        }
        Map<String, Object> visa = (Map) info.get("visa");
        if (visa != null) {
            // dict to JSON
            info.put("visa", JSON.encode(visa));
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
        ByteArray content = msg.getContent();
        if (content != null) {
            if (content.getByte(0) == '{') {
                // JsON
                info.put("data", content.toString());
            } else {
                // Base64
                info.put("data", Base64.encode(content.getBytes()));
            }
        }
        ByteArray signature = msg.getSignature();
        if (signature != null) {
            info.put("signature", Base64.encode(signature.getBytes()));
        }
        // symmetric key, keys
        ByteArray key = msg.getKey();
        if (key != null && key.getSize() > 5) {
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
        ByteArray meta = msg.getMeta();
        if (meta != null && meta.getSize() > 0) {
            // JsON to dict
            info.put("meta", JSON.decode(UTF8.decode(meta.getBytes())));
        }
        ByteArray visa = msg.getVisa();
        if (visa != null && visa.getSize() > 0) {
            // JsON to dict
            info.put("visa", JSON.decode(UTF8.decode(visa.getBytes())));
        }

        return ReliableMessage.parse(info);
    }

    private static ByteArray KEYS_PREFIX = new Data("KEYS:".getBytes());

    private static Map<String, Object> parseKeys(ByteArray data) {
        Map<String, Object> keys = new HashMap<>();
        IntegerData size;
        StringValue name;
        RawValue value;
        while (data.getSize() > 0) {
            // get key length
            size = VarIntData.from(data);
            data = data.slice(size.getSize());
            // get key name
            name = StringValue.from(data.slice(0, size.getIntValue()));
            data = data.slice(size.getIntValue());
            // get value length
            size = VarIntData.from(data);
            data = data.slice(size.getSize());
            // get value
            value = new RawValue(data.slice(0, size.getIntValue()));
            data = data.slice(size.getIntValue());
            if (name.getSize() > 0 && value.getSize() > 0) {
                keys.put(name.string, Base64.encode(value.getBytes()));
            }
        }
        return keys;
    }

    private static ByteArray buildKeys(Map<Object, String> keys) {
        MutableData data = new MutableData(512);
        Object key;
        String base64;
        StringValue idValue;
        RawValue keyValue;
        for (Map.Entry<Object, String> entry : keys.entrySet()) {
            key = entry.getKey();
            if (key instanceof ID) {
                idValue = StringValue.from(key.toString());
            } else if (key instanceof String) {
                idValue = StringValue.from((String) key);
            } else {
                assert key == null : "error key: " + key;
                continue;
            }
            base64 = entry.getValue();
            if (idValue.getSize() > 0 && base64 != null && base64.length() > 0) {
                keyValue = new RawValue(Base64.decode(base64));
                data.append(VarIntData.from(idValue.getSize()));
                data.append(idValue);
                data.append(VarIntData.from(keyValue.getSize()));
                data.append(keyValue);
            }
        }
        return data;
    }
}
