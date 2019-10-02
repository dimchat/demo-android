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

import chat.dim.common.Facebook;
import chat.dim.common.Messenger;
import chat.dim.core.Callback;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.ID;
import chat.dim.mkm.LocalUser;
import chat.dim.mkm.Meta;
import chat.dim.mkm.Profile;
import chat.dim.protocol.Command;
import chat.dim.protocol.command.MetaCommand;
import chat.dim.protocol.command.ProfileCommand;

class ContentDeliver {

    private Facebook facebook = Facebook.getInstance();

    Server server = null;

    /**
     *  Pack and send message content to receiver
     *
     * @param content - message content
     * @param receiver - contact/group ID
     * @return InstantMessage been sent
     */
    InstantMessage sendContent(Content content, ID receiver) {
        assert server != null;
        LocalUser user = server.getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        Facebook facebook = Facebook.getInstance();
        if (!receiver.isBroadcast() && facebook.getMeta(receiver) == null) {
            // NOTICE: if meta for sender not found,
            //         the client will query it automatically
            // TODO: save the message content in waiting queue
            return null;
        }
        // make instant message
        InstantMessage iMsg = new InstantMessage(content, user.identifier, receiver);
        // callback
        Callback callback = new Callback() {
            @Override
            public void onFinished(Object result, Error error) {
                String event;
                if (error == null) {
                    event = "MessageSent";
                    //iMsg.state = Accepted;
                } else {
                    event = "SendMessageFailed";
                    //iMsg.state = Error;
                    //iMsg.error = error;
                }
                // TODO: post notification with event name and message
            }
        };
        // send out
        Messenger messenger = Messenger.getInstance();
        if (messenger.sendMessage(iMsg, callback, true)) {
            return iMsg;
        }
        // error
        return null;
    }

    /**
     *  Pack and send command to station
     *
     * @param cmd - command should be sent to station
     * @return InstantMessage been sent
     */
    InstantMessage sendCommand(Command cmd) {
        assert server != null;
        return sendContent(cmd, server.identifier);
    }

    /**
     *  Pack and broadcast content to everyone
     *
     * @param content - message content
     * @return InstantMessage been sent
     */
    InstantMessage broadcastContent(Content content) {
        content.setGroup(ID.EVERYONE);
        return sendContent(content, ID.ANYONE);
    }

    //-------- commands

    void broadcastProfile(Profile profile) {
        assert server != null;
        LocalUser user = server.getCurrentUser();
        if (user == null) {
            // TODO: save the message content in waiting queue
            throw new IllegalStateException("login first");
        }
        assert profile.identifier.equals(user.identifier);
        // pack and send profile to every contact
        Command cmd = new ProfileCommand(profile.identifier, profile);
        List<ID> contacts = user.getContacts();
        for (ID contact : contacts) {
            sendContent(cmd, contact);
        }
    }

    InstantMessage postProfile(Profile profile) {
        return postProfile(profile, null);
    }

    InstantMessage postProfile(Profile profile, Meta meta) {
        Command cmd = new ProfileCommand(profile.identifier, meta, profile);
        return sendCommand(cmd);
    }

    void postContacts(List<ID> contacts) {
        // TODO: encrypt contacts and send to station
    }

    InstantMessage queryMeta(ID identifier) {
        if (identifier.isBroadcast()) {
            return null;
        }
        Command cmd = new MetaCommand(identifier);
        return sendCommand(cmd);
    }

    InstantMessage queryProfile(ID identifier) {
        if (identifier.isBroadcast()) {
            return null;
        }
        Command cmd = new ProfileCommand(identifier);
        return sendCommand(cmd);
    }

    InstantMessage queryOnlineUsers() {
        Command cmd = new Command("users");
        return sendCommand(cmd);
    }

    InstantMessage searchUsers(String keywords) {
        Command cmd = new Command("search");
        cmd.put("keywords", keywords);
        return sendCommand(cmd);
    }

    boolean login(LocalUser user) {
        assert server != null;
        if (user == null) {
            user = facebook.database.getCurrentUser();
            if (user == null) {
                // user not found
                return false;
            }
        }
        if (user.equals(server.getCurrentUser())) {
            // user not change
            return true;
        }
        // clear session
        server.session = null;

        server.setCurrentUser(user);

        server.handshake(null);
        return true;
    }
}
