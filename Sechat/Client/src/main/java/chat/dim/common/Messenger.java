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

import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.ReliableMessage;
import chat.dim.dkd.SecureMessage;
import chat.dim.mkm.ID;

public class Messenger extends chat.dim.Messenger {
    private static final Messenger ourInstance = new Messenger();
    public static Messenger getInstance() { return ourInstance; }
    private Messenger()  {
        super();

        setSocialNetworkDataSource(Facebook.getInstance());
        setCipherKeyDataSource(KeyStore.getInstance());
    }

    //-------- Convenient

    /**
     *  Pack instant message to reliable message for delivering
     *
     * @param iMsg - instant message
     * @return ReliableMessage Object
     */
    public ReliableMessage encryptAndSignMessage(InstantMessage iMsg) {

        // 1. encrypt 'content' to 'data' for receiver
        SecureMessage sMsg = encryptMessage(iMsg);

        // 1.1. check group
        Object group = iMsg.getGroup();
        if (group != null) {
            sMsg.setGroup(group);
        }

        // 2. sign 'data' by sender
        return signMessage(sMsg);
    }

    /**
     *  Extract instant message from a reliable message received
     *
     * @param rMsg - reliable message
     * @return InstantMessage object
     */
    public InstantMessage verifyAndDecryptMessage(ReliableMessage rMsg) {

        // 1. verify 'data' with 'signature'
        SecureMessage sMsg = verifyMessage(rMsg);

        // 2. check group message
        ID receiver = getID(sMsg.envelope.receiver);
        if (receiver.getType().isGroup()) {
            // TODO: split it
        }

        // 3. decrypt 'data' to 'content'
        return decryptMessage(sMsg);
    }
}
