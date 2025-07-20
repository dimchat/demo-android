/* license: https://mit-license.org
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2022 Albert Moky
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
package chat.dim.cpu;

import chat.dim.Facebook;
import chat.dim.Messenger;
import chat.dim.cpu.app.CustomizedContentHandler;
import chat.dim.cpu.customized.DriftBottleHandler;
import chat.dim.protocol.CustomizedContent;
import chat.dim.protocol.ReliableMessage;

/**
 *  Application Customized Content Processor
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *  Process customized contents for this application only
 */
public class AppContentProcessor extends AppCustomizedContentProcessor {

    // module(s) for customized contents
    private final DriftBottleHandler driftBottle;

    public AppContentProcessor(Facebook facebook, Messenger messenger) {
        super(facebook, messenger);
        driftBottle = new DriftBottleHandler(facebook, messenger);
    }

    @Override
    protected CustomizedContentHandler filter(String app, String mod, CustomizedContent content, ReliableMessage rMsg) {
        if (driftBottle.matches(app, mod)) {
            // customized module: "drift_bottle"
            return driftBottle;
        }
        // TODO: define your modules here
        // ...

        return super.filter(app, mod, content, rMsg);
    }
}
