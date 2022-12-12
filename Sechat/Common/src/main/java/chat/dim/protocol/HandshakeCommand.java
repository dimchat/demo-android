/* license: https://mit-license.org
 *
 *  DIMP : Decentralized Instant Messaging Protocol
 *
 *                                Written in 2019 by Moky <albert.moky@gmail.com>
 *
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

/**
 *  Handshake command message: {
 *      type : 0x88,
 *      sn   : 123,
 *
 *      cmd     : "handshake",    // command name
 *      title   : "Hello world!", // "DIM?", "DIM!"
 *      session : "{SESSION_KEY}" // session key
 *  }
 */
public class HandshakeCommand extends BaseCommand {

    public final static String HANDSHAKE = "handshake";

    public HandshakeCommand(Map<String, Object> command) {
        super(command);
    }

    public HandshakeCommand(String text, String session) {
        super(HANDSHAKE);
        // text message
        put("title", text);
        put("message", text);  // TODO: remove after all servers upgraded
        // session key
        if (session != null) {
            put("session", session);
        }
    }

    public String getTitle() {
        // TODO: modify after all servers upgraded
        Object text = get("title");
        if (text == null) {
            // compatible with v1.0
            text = get("message");
        }
        return (String) text;
    }

    public String getSessionKey() {
        return (String) get("session");
    }

    public HandshakeState getState() {
        return checkState(getTitle(), getSessionKey());
    }

    private static HandshakeState checkState(String text, String session) {
        assert text != null : "handshake title should not be empty";
        if (text.equals("DIM!") || text.equals("OK!")) {
            return HandshakeState.SUCCESS;
        } else if (text.equals("DIM?")) {
            return HandshakeState.AGAIN;
        } else if (session == null) {
            return HandshakeState.START;
        } else {
            return HandshakeState.RESTART;
        }
    }

    //
    //  Factories
    //

    public static HandshakeCommand start() {
        return new HandshakeCommand("Hello world!", null);
    }

    public static HandshakeCommand restart(String sessionKey) {
        return new HandshakeCommand("Hello world!", sessionKey);
    }

    public static HandshakeCommand again(String sessionKey) {
        return new HandshakeCommand("DIM?", sessionKey);
    }

    public static HandshakeCommand success(String sessionKey) {
        return new HandshakeCommand("DIM!", sessionKey);
    }
}
