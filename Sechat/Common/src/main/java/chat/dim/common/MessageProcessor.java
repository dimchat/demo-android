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
package chat.dim.common;

import java.util.ArrayList;
import java.util.List;

import chat.dim.cpu.AnyContentProcessor;
import chat.dim.cpu.BlockCommandProcessor;
import chat.dim.cpu.CommandProcessor;
import chat.dim.cpu.ContentProcessor;
import chat.dim.cpu.MuteCommandProcessor;
import chat.dim.cpu.ReceiptCommandProcessor;
import chat.dim.protocol.BlockCommand;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.ID;
import chat.dim.protocol.Meta;
import chat.dim.protocol.MuteCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.ReportCommand;
import chat.dim.protocol.SearchCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.ResetCommand;

public class MessageProcessor extends chat.dim.MessageProcessor {

    public MessageProcessor(Facebook facebook, Messenger messenger, MessagePacker packer) {
        super(facebook, messenger, packer);
    }

    protected Messenger getMessenger() {
        return (Messenger) super.getMessenger();
    }

    protected Facebook getFacebook() {
        return (Facebook) super.getFacebook();
    }

    // check whether group info empty
    private boolean isEmpty(ID group) {
        chat.dim.Facebook facebook = getFacebook();
        List members = facebook.getMembers(group);
        if (members == null || members.size() == 0) {
            return true;
        }
        ID owner = facebook.getOwner(group);
        return owner == null;
    }

    // check whether need to update group
    public boolean checkGroup(Content content, ID sender) {
        // Check if it is a group message, and whether the group members info needs update
        ID group = content.getGroup();
        if (group == null || ID.isBroadcast(group)) {
            // 1. personal message
            // 2. broadcast message
            return false;
        }
        // check meta for new group ID
        chat.dim.Facebook facebook = getFacebook();
        Meta meta = facebook.getMeta(group);
        if (meta == null) {
            // NOTICE: if meta for group not found,
            //         facebook should query it from DIM network automatically
            // TODO: insert the message to a temporary queue to wait meta
            //throw new NullPointerException("group meta not found: " + group);
            return true;
        }
        // query group info
        if (isEmpty(group)) {
            // NOTICE: if the group info not found, and this is not an 'invite' command
            //         query group info from the sender
            if (content instanceof InviteCommand || content instanceof ResetCommand) {
                // FIXME: can we trust this stranger?
                //        may be we should keep this members list temporary,
                //        and send 'query' to the owner immediately.
                // TODO: check whether the members list is a full list,
                //       it should contain the group owner(owner)
                return false;
            } else {
                return getMessenger().queryGroupInfo(group, sender);
            }
        } else if (facebook.containsMember(sender, group)
                || facebook.containsAssistant(sender, group)
                || facebook.isOwner(sender, group)) {
            // normal membership
            return false;
        } else {

            // if assistants exists, query them
            List<ID> assistants = facebook.getAssistants(group);
            List<ID> admins = new ArrayList<>(assistants);
            // if owner found, query it too
            ID owner = facebook.getOwner(group);
            if (owner != null && !admins.contains(owner)) {
                admins.add(owner);
            }
            return getMessenger().queryGroupInfo(group, admins);
        }
    }

    @Override
    protected Content process(Content content, ReliableMessage rMsg) {
        ID sender = rMsg.getSender();
        if (checkGroup(content, sender)) {
            // save this message in a queue to wait group meta response
            ID group = content.getGroup();
            rMsg.put("waiting", group);
            getMessenger().suspendMessage(rMsg);
            return null;
        }
        try {
            return super.process(content, rMsg);
        } catch (NullPointerException e) {
            e.printStackTrace();
            String text = e.getMessage();
            if (text.contains("failed to get meta for ")) {
                int pos = text.indexOf(": ");
                if (pos > 0) {
                    ID waiting = ID.parse(text.substring(pos + 2));
                    if (waiting == null) {
                        throw new NullPointerException("failed to get ID: " + text);
                    } else {
                        rMsg.put("waiting", waiting);
                        getMessenger().suspendMessage(rMsg);
                    }
                }
            }
            return null;
        }
    }

    static {
        // register command parsers
        Command.register(SearchCommand.SEARCH, SearchCommand::new);
        Command.register(SearchCommand.ONLINE_USERS, SearchCommand::new);

        Command.register(ReportCommand.REPORT, ReportCommand::new);
        Command.register(ReportCommand.ONLINE, ReportCommand::new);
        Command.register(ReportCommand.OFFLINE, ReportCommand::new);

        // register content processors
        ContentProcessor.register(0, new AnyContentProcessor());

        // register command processors
        CommandProcessor.register(Command.RECEIPT, new ReceiptCommandProcessor());
        CommandProcessor.register(MuteCommand.MUTE, new MuteCommandProcessor());
        CommandProcessor.register(BlockCommand.BLOCK, new BlockCommandProcessor());
    }
}
