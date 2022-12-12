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

import chat.dim.dkd.BaseCommand;

/**
 *  Receipt command message: {
 *      type : 0x88,
 *      sn   : 456,
 *
 *      cmc    : "receipt",
 *      text   : "...",  // text message
 *      origin : {       // original message envelope
 *          sender    : "...",
 *          receiver  : "...",
 *          time      : 0,
 *          sn        : 123,
 *          signature : "..."
 *      }
 *  }
 */
public class ReceiptCommand extends BaseCommand {

    public static final String RECEIPT   = "receipt";

    // original message envelope
    private Envelope envelope;

    public ReceiptCommand(Map<String, Object> dictionary) {
        super(dictionary);
        envelope = null;
    }

    public ReceiptCommand(String text, Envelope env, long sn, String signature) {
        super(RECEIPT);
        // text message
        if (text != null) {
            put("text", text);
        }
        envelope = env;
        // envelope of the message responding to
        Map<String, Object> origin;
        if (env == null) {
            origin = new HashMap<>();
        } else {
            origin = env.copyMap(false);
        }
        // sn of the message responding to
        if (sn > 0) {
            origin.put("sn", sn);
        }
        // signature of the message responding to
        if (signature != null) {
            origin.put("signature", signature);
        }
        put("origin", origin);
    }

    //-------- setters/getters --------

    public String getText() {
        return (String) get("text");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrigin() {
        Object origin = get("origin");
        return origin == null ? null : (Map<String, Object>) origin;
    }

    public Envelope getOriginalEnvelope() {
        if (envelope == null) {
            // origin: { sender: "...", receiver: "...", time: 0 }
            Map<String, Object> origin = getOrigin();
            if (origin != null && origin.get("sender") != null) {
                envelope = Envelope.parse(origin);
            }
        }
        return envelope;
    }

    public long getOriginalSerialNumber() {
        Map<String, Object> origin = getOrigin();
        if (origin == null) {
            return 0;
        }
        return (long) origin.get("sn");
    }

    public String getOriginalSignature() {
        Map<String, Object> origin = getOrigin();
        if (origin == null) {
            return null;
        }
        return (String) origin.get("signature");
    }
}
