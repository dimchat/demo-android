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
package chat.dim.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import chat.dim.database.ConversationTable;
import chat.dim.database.MessageTable;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.dkd.Message;
import chat.dim.mkm.ID;
import chat.dim.notification.NotificationCenter;
import chat.dim.protocol.Command;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.utils.StringUtils;

public class ConversationDatabase implements ConversationDataSource {
    private static final ConversationDatabase ourInstance = new ConversationDatabase();
    public static ConversationDatabase getInstance() { return ourInstance; }
    private ConversationDatabase() {
        super();
        Amanuensis.getInstance().database = this;
    }

    // constants
    public static final String MessageUpdated = "MessageUpdated";
    public static final String MessageCleaned = "MessageCleaned";

    private ConversationTable conversationTable = new ConversationTable();
    private MessageTable messageTable = new MessageTable();

    public String getTimeString(Message msg) {
        Date time = msg.envelope.time;
        if (time == null) {
            return null;
        }
        return getTimeString(time);
    }

    private String getTimeString(Date time) {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
        return formatter.format(time);
    }

    public String getContentText(Content content) {
        return MessageBuilder.getContentText(content);
    }

    public String getCommandText(Command cmd, ID sender) {
        return MessageBuilder.getCommandText(cmd, sender);
    }

    //-------- ConversationDataSource

    @Override
    public int numberOfConversations() {
        return conversationTable.numberOfConversations();
    }

    @Override
    public ID conversationAtIndex(int index) {
        return conversationTable.conversationAtIndex(index);
    }

    @Override
    public boolean removeConversationAtIndex(int index) {
        return conversationTable.removeConversationAtIndex(index);
    }

    @Override
    public boolean removeConversation(ID identifier) {
        return conversationTable.removeConversation(identifier);
    }

    // messages

    public List<InstantMessage> messagesInConversation(Conversation chatBox) {
        return messageTable.messagesInConversation(chatBox);
    }

    @Override
    public int numberOfMessages(Conversation chatBox) {
        return messageTable.numberOfMessages(chatBox);
    }

    @Override
    public InstantMessage messageAtIndex(int index, Conversation chatBox) {
        return messageTable.messageAtIndex(index, chatBox);
    }

    private void postMessageUpdatedNotification(InstantMessage iMsg, Conversation chatBox) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("ID", chatBox.identifier);
        userInfo.put("msg", iMsg);
        NotificationCenter nc = NotificationCenter.getInstance();
        nc.postNotification(MessageUpdated, this, userInfo);
    }

    @Override
    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.insertMessage(iMsg, chatBox);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox);
        }
        return OK;
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.removeMessage(iMsg, chatBox);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox);
        }
        return OK;
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        boolean OK = messageTable.withdrawMessage(iMsg, chatBox);
        if (OK) {
            postMessageUpdatedNotification(iMsg, chatBox);
        }
        return OK;
    }

    @Override
    public boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        boolean OK = messageTable.saveReceipt(receipt, chatBox);
        if (OK) {
            postMessageUpdatedNotification(receipt, chatBox);
        }
        return OK;
    }
}

class MessageBuilder {

    private static String getUsername(Object string) {
        return Facebook.getInstance().getUsername(string);
    }

    //-------- Content

    public static String getContentText(Content content) {
        if (content instanceof TextContent) {
            TextContent text = (TextContent) content;
            return text.getText();
        }
        // File: Image, Audio, Video
        if (content instanceof FileContent) {
            // text should be built by CPUs already
            return (String) content.get("text");
        }
        return String.format(Locale.CHINA, "Current version doesn't support this message type: %s", content.type);
    }

    //-------- Command

    public static String getCommandText(Command cmd, ID commander) {
        if (cmd instanceof GroupCommand) {
            return getGroupCommandText((GroupCommand) cmd, commander);
        }
        if (cmd instanceof HistoryCommand) {
            // TODO: process history command
        }

        // receipt
        if (cmd instanceof ReceiptCommand) {
            ReceiptCommand receipt = (ReceiptCommand) cmd;
            return receipt.getMessage();
        }

        return String.format(Locale.CHINA, "Current version doesn't support this command: %s", cmd.command);
    }

    //-------- System commands

    //...

    //-------- Group Commands

    private static String getGroupCommandText(GroupCommand cmd, ID commander) {
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
        throw new UnsupportedOperationException("unsupported group command: " + cmd);
    }

    private static String getInviteCommandText(InviteCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }
        List addedList = (List) cmd.get("added");
        if (addedList == null || addedList.size() == 0) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Object item : addedList) {
            names.add(getUsername(item));
        }
        String string = StringUtils.join(names, ", ");

        text = String.format(Locale.CHINA, "%s has invited members: %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private static String getExpelCommandText(ExpelCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }
        List removedList = (List) cmd.get("removed");
        if (removedList == null || removedList.size() == 0) {
            return null;
        }
        List<String> names = new ArrayList<>();
        for (Object item : removedList) {
            names.add(getUsername(item));
        }
        String string = StringUtils.join(names, ", ");

        text = String.format(Locale.CHINA, "%s has removed members: %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private static String getQuitCommandText(QuitCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }

        text = String.format(Locale.CHINA, "%s has quit group chat.", getUsername(commander));
        cmd.put("text", text);
        return text;
    }

    private static String getResetCommandText(ResetCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }
        List addedList = (List) cmd.get("added");
        List removedList = (List) cmd.get("removed");

        String string = "";
        if (removedList != null && removedList.size() > 0) {
            List<String> names = new ArrayList<>();
            for (Object item : removedList) {
                names.add(getUsername(item));
            }
            string = string + ", removed: " + StringUtils.join(names, ", ");
        }
        if (addedList != null && addedList.size() > 0) {
            List<String> names = new ArrayList<>();
            for (Object item : addedList) {
                names.add(getUsername(item));
            }
            string = string + ", added: " + StringUtils.join(names, ", ");
        }

        text = String.format(Locale.CHINA, "%s has updated members %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private static String getQueryCommandText(QueryCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }

        text = String.format(Locale.CHINA, "%s was querying group info, responding...", getUsername(commander));
        cmd.put("text", text);
        return text;
    }
}
