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
package chat.dim.cpu;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.utils.Strings;

class MessageBuilder {

    private static String getUsername(Object string) {
        return AnyContentProcessor.facebook.getUsername(string);
    }

    //-------- System commands

    static String getLoginCommandText(LoginCommand cmd, ID commander) {
        assert commander != null : "commander error";
        ID identifier = cmd.getIdentifier();
        Map<String, Object> station = cmd.getStation();
        return String.format("%s login: %s", getUsername(identifier), station);
    }

    //...

    //-------- Group Commands

    static String getGroupCommandText(GroupCommand cmd, ID commander) {
        if (cmd instanceof InviteCommand) {
            return getInviteCommandText((InviteCommand) cmd, commander);
        }
        if (cmd instanceof ExpelCommand) {
            return getExpelCommandText((ExpelCommand) cmd, commander);
        }
        if (cmd instanceof QuitCommand) {
            return getQuitCommandText((QuitCommand) cmd, commander);
        }
        if (cmd instanceof ResetCommand) {
            return getResetCommandText((ResetCommand) cmd, commander);
        }
        if (cmd instanceof QueryCommand) {
            return getQueryCommandText((QueryCommand) cmd, commander);
        }
        return String.format("unsupported group command: %s", cmd);
    }

    private static String getInviteCommandText(InviteCommand cmd, ID commander) {
        List addedList = (List) cmd.get("added");
        if (addedList == null) {
            addedList = new ArrayList();
        }
        List<String> names = new ArrayList<>();
        for (Object item : addedList) {
            names.add(getUsername(item));
        }
        String string = Strings.join(names, ", ");
        return String.format("%s has invited members: %s", getUsername(commander), string);
    }

    private static String getExpelCommandText(ExpelCommand cmd, ID commander) {
        List removedList = (List) cmd.get("removed");
        if (removedList == null) {
            removedList = new ArrayList();
        }
        List<String> names = new ArrayList<>();
        for (Object item : removedList) {
            names.add(getUsername(item));
        }
        String string = Strings.join(names, ", ");
        return String.format("%s has removed members: %s", getUsername(commander), string);
    }

    private static String getQuitCommandText(QuitCommand cmd, ID commander) {
        assert cmd.getGroup() != null : "quit command error: " + cmd;
        return String.format("%s has quit group chat.", getUsername(commander));
    }

    private static String getResetCommandText(ResetCommand cmd, ID commander) {
        List addedList = (List) cmd.get("added");
        List removedList = (List) cmd.get("removed");

        String string = "";
        if (removedList != null && removedList.size() > 0) {
            List<String> names = new ArrayList<>();
            for (Object item : removedList) {
                names.add(getUsername(item));
            }
            string = string + ", removed: " + Strings.join(names, ", ");
        }
        if (addedList != null && addedList.size() > 0) {
            List<String> names = new ArrayList<>();
            for (Object item : addedList) {
                names.add(getUsername(item));
            }
            string = string + ", added: " + Strings.join(names, ", ");
        }
        return String.format("%s has updated members %s", getUsername(commander), string);
    }

    private static String getQueryCommandText(QueryCommand cmd, ID commander) {
        assert cmd.getGroup() != null : "quit command error: " + cmd;
        return String.format("%s was querying group info, responding...", getUsername(commander));
    }
}
