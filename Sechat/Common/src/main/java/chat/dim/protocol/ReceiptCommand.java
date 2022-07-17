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
package chat.dim.protocol;

import java.util.Map;

import chat.dim.dkd.BaseCommand;
import chat.dim.format.Base64;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,  // the same serial number with the original message
 *
 *      command   : "receipt",
 *      message   : "...",
 *      // -- extra info
 *      sender    : "...",
 *      receiver  : "...",
 *      time      : 0,
 *      signature : "..." // the same signature with the original message
 *  }
 */
public class ReceiptCommand extends BaseCommand {

    public static final String RECEIPT   = "receipt";

    // original message info
    private Envelope envelope;

    public ReceiptCommand(Map<String, Object> dictionary) {
        super(dictionary);
        envelope = null;
    }

    public ReceiptCommand(String message, long sn, Envelope env) {
        super(RECEIPT);
        // text
        if (message != null) {
            put("message", message);
        }
        // sn of the message responding to
        if (sn > 0) {
            put("sn", sn);
        }
        // envelope of the message responding to
        if (env != null) {
            setEnvelope(env);
        }
        envelope = env;
    }

    //-------- setters/getters --------

    public String getMessage() {
        return (String) get("message");
    }

    public Envelope getEnvelope() {
        if (envelope == null) {
            // envelope: { sender: "...", receiver: "...", time: 0 }
            Object env = get("envelope");
            if (env == null) {
                Object sender = get("sender");
                Object receiver = get("receiver");
                if (sender != null && receiver != null) {
                    env = toMap();
                }
            }
            envelope = Envelope.parse(env);
        }
        return envelope;
    }

    public void setEnvelope(Envelope env) {
        if (env == null) {
            remove("sender");
            remove("receiver");
            //remove("time");
        } else {
            put("sender", env.get("sender"));
            put("receiver", env.get("receiver"));
            /*/
            Object time = env.get("time");
            if (time != null) {
                put("time", time);
            }
            /*/
            Object group = env.get("group");
            if (group != null) {
                put("group", group);
            }
        }
    }

    public byte[] getSignature() {
        String signature = (String) get("signature");
        if (signature == null) {
            return null;
        }
        return Base64.decode(signature);
    }

    public void setSignature(byte[] signature) {
        if (signature == null) {
            remove("signature");
        } else {
            put("signature", Base64.encode(signature));
        }
    }
}
