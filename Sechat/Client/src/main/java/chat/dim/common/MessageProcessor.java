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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import chat.dim.database.ConversationDatabase;
import chat.dim.dkd.Content;
import chat.dim.dkd.Message;
import chat.dim.mkm.ID;
import chat.dim.mkm.Profile;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.VideoContent;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.utils.StringUtils;

public class MessageProcessor extends ConversationDatabase {
    private static final MessageProcessor ourInstance = new MessageProcessor();
    public static MessageProcessor getInstance() { return ourInstance; }
    private MessageProcessor() {
        super();
        Amanuensis.getInstance().database = this;
    }

    private Facebook facebook = Facebook.getInstance();

    private String getUsername(Object string) {
        return getUsername(facebook.getID(string));
    }

    private String getUsername(ID identifier) {
        Profile profile = facebook.getProfile(identifier);
        String nickname = profile == null ? null : profile.getName();
        String username = identifier.name;
        if (nickname != null) {
            if (username != null && identifier.getType().isUser()) {
                return nickname + " (" + username + ")";
            }
            return nickname;
        } else if (username != null) {
            return username;
        } else {
            // BTC address
            return identifier.address.toString();
        }
    }

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

    //-------- Content

    public String getContentText(Content content) {
        if (content instanceof TextContent) {
            TextContent text = (TextContent) content;
            return text.getText();
        }
        // File: Image, Audio, Video
        if (content instanceof FileContent) {
            FileContent file = (FileContent) content;
            if (content instanceof ImageContent) {
                return String.format("[Image:%s]", file.getFilename());
            }
            if (content instanceof AudioContent) {
                return String.format("[Voice:%s]", file.getFilename());
            }
            if (content instanceof VideoContent) {
                return String.format("[Movie:%s]", file.getFilename());
            }
            return String.format("[File:%s]", file.getFilename());
        }
        return String.format(Locale.CHINA,"Current version doesn't support this message type(%d)", content.type);
    }

    //-------- Command

    public String getCommandText(Command cmd, ID commander) {
        if (cmd instanceof GroupCommand) {
            return getGroupCommandText((GroupCommand) cmd, commander);
        }
        if (cmd instanceof HistoryCommand) {
            // TODO: process history command
        }
        return String.format("Current version doesn't support this command(%s)", cmd.command);
    }

    //-------- System commands

    //...

    //-------- Group Commands

    private String getGroupCommandText(GroupCommand cmd, ID commander) {
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

    private String getInviteCommandText(InviteCommand cmd, ID commander) {
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

        text = String.format("%s has invited members: %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private String getExpelCommandText(ExpelCommand cmd, ID commander) {
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

        text = String.format("%s has removed members: %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private String getQuitCommandText(QuitCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }

        text = String.format("%s has quit group chat.", getUsername(commander));
        cmd.put("text", text);
        return text;
    }

    private String getResetCommandText(ResetCommand cmd, ID commander) {
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

        text = String.format("%s has updated members %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private String getQueryCommandText(QueryCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }

        text = String.format("%s was querying group info, responding...", getUsername(commander));
        cmd.put("text", text);
        return text;
    }
}
