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

import java.util.HashMap;
import java.util.Map;

import chat.dim.format.Base64;
import chat.dim.dkd.Envelope;

/**
 *  Command message: {
 *      type : 0x88,
 *      sn   : 123,  // the same serial number with the original message
 *
 *      command : "receipt",
 *      message : "...",
 *      // -- extra info
 *      sender    : "...",
 *      receiver  : "...",
 *      time      : 0,
 *      signature : "..." // the same signature with the original message
 *  }
 */
public class ReceiptCommand extends Command {

    public final String message;

    // original message info
    private Envelope envelope;
    private byte[] signature;

    public ReceiptCommand(Map<String, Object> dictionary) {
        super(dictionary);
        message = (String) dictionary.get("message");

        // -- extra info
        // envelope: { sender: "...", receiver: "...", time: 0 }
        Object env = dictionary.get("envelope");
        if (env == null) {
            Object sender = dictionary.get("sender");
            Object receiver = dictionary.get("receiver");
            Object time = dictionary.get("time");
            if (sender != null && receiver != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("sender", sender);
                map.put("receiver", receiver);
                map.put("time", time);
                env = map;
            }
        }
        envelope = Envelope.getInstance(env);

        // signature
        String base64 = (String) dictionary.get("signature");
        if (base64 == null) {
            signature = null;
        } else {
            signature = Base64.decode(base64);
        }
    }

    public ReceiptCommand(String text) {
        super(RECEIPT);
        message   = text;
        envelope  = null;
        signature = null;
        dictionary.put("message", text);
    }

    //-------- setters/getters --------

    public void setEnvelope(Envelope env) {
        envelope = env;
        if (env == null) {
            dictionary.remove("envelope");
            dictionary.remove("sender");
            dictionary.remove("receiver");
            dictionary.remove("time");
        } else {
            dictionary.put("sender", env.sender);
            dictionary.put("receiver", env.receiver);
            dictionary.put("time", env.time);
        }
    }

    public Envelope getEnvelope() {
        return envelope;
    }

    public void setSignature(byte[] sig) {
        signature = sig;
        if (sig == null) {
            dictionary.remove("signature");
        } else {
            dictionary.put("signature", Base64.encode(sig));
        }
    }

    public byte[] getSignature() {
        return signature;
    }
}
