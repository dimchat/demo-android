/* license: https://mit-license.org
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
package chat.dim.cpu;

import java.util.List;
import java.util.Map;

import chat.dim.database.LoginTable;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.utils.Log;
import chat.dim.utils.Times;

public class LoginCommandProcessor extends CommandProcessor {

    public LoginCommandProcessor() {
        super();
    }

    @Override
    public List<Content> execute(Command cmd, ReliableMessage rMsg) {
        assert cmd instanceof LoginCommand : "login command error: " + cmd;
        LoginCommand lCmd = (LoginCommand) cmd;

        // update contact's login status
        loginTable.saveLoginCommand(lCmd);

        ID identifier = lCmd.getIdentifier();
        Map<String, Object> station = lCmd.getStation();
        Log.info("[" + Times.getTimeString(cmd.getTime()) + "] user (" + identifier + ") login: " + station);

        // no need to response login command
        return null;
    }

    public static LoginTable loginTable = null;
}
