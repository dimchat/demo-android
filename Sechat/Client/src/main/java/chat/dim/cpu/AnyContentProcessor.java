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

import chat.dim.Content;
import chat.dim.ID;
import chat.dim.InstantMessage;
import chat.dim.Messenger;
import chat.dim.model.Facebook;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.HistoryCommand;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.VideoContent;
import chat.dim.protocol.group.ExpelCommand;
import chat.dim.protocol.group.InviteCommand;
import chat.dim.protocol.group.QueryCommand;
import chat.dim.protocol.group.QuitCommand;
import chat.dim.protocol.group.ResetCommand;
import chat.dim.utils.StringUtils;

public class AnyContentProcessor extends DefaultContentProcessor {

    public AnyContentProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
    public Content process(Content content, ID sender, InstantMessage iMsg) {
        String text;

        // File: Image, Audio, Video
        if (content instanceof FileContent) {
            if (content instanceof ImageContent) {
                // Image
                text = "Image received";
            } else if (content instanceof AudioContent) {
                // Audio
                text = "Voice message received";
            } else if (content instanceof VideoContent) {
                // Video
                text = "Movie received";
            } else {
                // other file
                text = "File received";
            }
        } else if (content instanceof TextContent) {
            // Text
            assert content.get("text") != null;
            text = "Text message received";
        } else if (content instanceof PageContent) {
            // Web page
            assert content.get("URL") != null;
            text = "Web page received";
        } else {
            // Other
            return super.process(content, sender, iMsg);
        }

        // response
        Object group = content.getGroup();
        if (group == null) {
            return new ReceiptCommand(text, iMsg.envelope, content.serialNumber);
        } else {
            // DON'T response group message for disturb reason
            return null;
        }
    }

    //
    //  Text Builder
    //

    public static String getContentText(Content content) {
        // File: Image, Audio, Video
        if (content instanceof FileContent) {
            if (content instanceof ImageContent) {
                ImageContent image = (ImageContent) content;
                return String.format("[Image:%s]", image.getFilename());
            }
            if (content instanceof AudioContent) {
                AudioContent audio = (AudioContent) content;
                return String.format("[Voice:%s]", audio.getFilename());
            }
            if (content instanceof VideoContent) {
                VideoContent video = (VideoContent) content;
                return String.format("[Movie:%s]", video.getFilename());
            }
            FileContent file = (FileContent) content;
            return String.format("[File:%s]", file.getFilename());
        }
        // Text
        if (content instanceof TextContent) {
            TextContent text = (TextContent) content;
            return text.getText();
        }
        // Web page
        if (content instanceof PageContent) {
            PageContent page = (PageContent) content;
            return String.format("[URL:%s]", page.getUrl());
        }
        return String.format("Current version doesn't support this message type: %s", content.type);
    }

    public static String getCommandText(Command cmd, ID commander) {
        if (cmd instanceof GroupCommand) {
            return MessageBuilder.getGroupCommandText((GroupCommand) cmd, commander);
        }
        if (cmd instanceof HistoryCommand) {
            // TODO: process history command
        }

        return String.format("Current version doesn't support this command: %s", cmd.command);
    }
}

class MessageBuilder {

    private static String getUsername(Object string) {
        return Facebook.getInstance().getUsername(string);
    }

    //-------- System commands

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

        text = String.format("%s has invited members: %s", getUsername(commander), string);
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

        text = String.format("%s has removed members: %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private static String getQuitCommandText(QuitCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }

        text = String.format("%s has quit group chat.", getUsername(commander));
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

        text = String.format("%s has updated members %s", getUsername(commander), string);
        cmd.put("text", text);
        return text;
    }

    private static String getQueryCommandText(QueryCommand cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }

        text = String.format("%s was querying group info, responding...", getUsername(commander));
        cmd.put("text", text);
        return text;
    }
}
