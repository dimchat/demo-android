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
package chat.dim.network;

import java.util.List;
import java.util.Map;

import chat.dim.common.Facebook;
import chat.dim.mkm.ID;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.protocol.Command;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.command.HandshakeCommand;
import chat.dim.protocol.command.MetaCommand;
import chat.dim.protocol.command.ProfileCommand;
import chat.dim.protocol.group.GroupCommand;

class CommandProcessor {

    private Facebook facebook = Facebook.getInstance();

    private GroupCommandProcessor gCmd = new GroupCommandProcessor();
    private HistoryCommandProcessor hCmd = new HistoryCommandProcessor();

    Server server = null;

    boolean process(Command cmd, ID sender) {
        // group commands
        if (cmd instanceof GroupCommand) {
            return gCmd.process((GroupCommand) cmd, sender);
        }

        // history commands
        if (cmd instanceof HistoryCommand) {
            return hCmd.process((HistoryCommand) cmd);
        }

        // commands
        if (cmd instanceof HandshakeCommand) {
            // handshake
            return processHandshake((HandshakeCommand) cmd);
        }
        if (cmd instanceof ProfileCommand) {
            // query profile response
            return processProfile((ProfileCommand) cmd);
        }
        if (cmd instanceof MetaCommand) {
            // query meta response
            return processMeta((MetaCommand) cmd);
        }

        String command = cmd.command;
        if (command.equalsIgnoreCase("users")) {
            // query online users response
            return processOnlineUsers(cmd);
        }
        if (command.equalsIgnoreCase("search")) {
            // query search users response
            return processSearchUsers(cmd);
        }
        if (command.equalsIgnoreCase(Command.RECEIPT)) {
            // receipt
            return processReceipt(cmd);
        }

        // NOTE: let the message processor to do the job
        return false;
    }

    private boolean processHandshake(HandshakeCommand cmd) {
        int state = cmd.state;
        if (state == HandshakeCommand.AGAIN) {
            // update session and handshake again
            server.handshake(cmd.sessionKey);
            return true;
        }
        if (state == HandshakeCommand.SUCCESS) {
            // handshake OK
            server.handshakeAccepted(null, true);
            return true;
        }
        // handshake rejected
        server.handshakeAccepted(cmd.sessionKey, false);
        return false;
    }

    private boolean processMeta(MetaCommand cmd) {
        // check meta
        ID identifier = cmd.identifier;
        Meta meta = cmd.meta;
        if (meta == null) {
            // TODO: query meta?
            return false;
        }
        if (!meta.matches(identifier)) {
            throw new IllegalArgumentException("meta error: " + cmd);
        }
        // got new meta
        return facebook.saveMeta(meta, identifier);
    }

    private boolean processProfile(ProfileCommand cmd) {
        // check meta in profile command
        processMeta(cmd);

        // check profile
        ID identifier = cmd.identifier;
        Profile profile = cmd.profile;
        if (profile == null) {
            // TODO: query profile?
            return false;
        }
        if (!identifier.equals(profile.identifier)) {
            throw new IllegalArgumentException("profile error: " + cmd);
        }
        // got new profile
        // TODO: postNotification("ProfileUpdated")
        return facebook.saveProfile(profile);
    }

    private boolean processOnlineUsers(Command cmd) {
        List users = (List) cmd.get("users");
        // TODO: postNotification("OnlineUsersUpdated");
        return false;
    }

    private boolean processSearchUsers(Command cmd) {
        List users = (List) cmd.get("users");
        Map results = (Map) cmd.get("results");
        // TODO: postNotification("SearchUsersUpdated")
        return false;
    }

    private boolean processReceipt(Command cmd) {
        // TODO: process receipt
        return false;
    }
}


