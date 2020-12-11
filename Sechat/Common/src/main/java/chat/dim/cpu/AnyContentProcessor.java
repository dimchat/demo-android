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

import chat.dim.Messenger;
import chat.dim.common.Facebook;
import chat.dim.protocol.AudioContent;
import chat.dim.protocol.Command;
import chat.dim.protocol.Content;
import chat.dim.protocol.FileContent;
import chat.dim.protocol.GroupCommand;
import chat.dim.protocol.ID;
import chat.dim.protocol.ImageContent;
import chat.dim.protocol.LoginCommand;
import chat.dim.protocol.PageContent;
import chat.dim.protocol.ReceiptCommand;
import chat.dim.protocol.ReliableMessage;
import chat.dim.protocol.TextContent;
import chat.dim.protocol.VideoContent;

public class AnyContentProcessor extends ContentProcessor {

    public AnyContentProcessor(Messenger messenger) {
        super(messenger);
    }

    @Override
    protected Content unknown(Content content, ReliableMessage rMsg) {
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
            text = "Text message received";
        } else if (content instanceof PageContent) {
            // Web page
            text = "Web page received";
        } else {
            text = "Content (type: " + content.getType() + ") not support yet!";
            TextContent res = new TextContent(text);
            ID group = content.getGroup();
            if (group != null) {
                res.setGroup(group);
            }
            return res;
        }

        Object group = content.getGroup();
        if (group != null) {
            // DON'T response group message for disturb reason
            return null;
        }

        // response
        Object signature = rMsg.get("signature");
        ReceiptCommand receipt = new ReceiptCommand(text, content.getSerialNumber(), rMsg.getEnvelope());
        receipt.put("signature", signature);
        return receipt;
    }

    //
    //  Text Builder
    //

    public static String getContentText(Content content) {
        String text = (String) content.get("text");
        if (text != null) {
            return text;
        }
        if (content instanceof TextContent) {
            // Text
            return ((TextContent) content).getText();
        } else if (content instanceof FileContent) {
            // File: Image, Audio, Video
            if (content instanceof ImageContent) {
                ImageContent image = (ImageContent) content;
                text = String.format("[Image:%s]", image.getFilename());
            } else if (content instanceof AudioContent) {
                AudioContent audio = (AudioContent) content;
                text = String.format("[Voice:%s]", audio.getFilename());
            } else if (content instanceof VideoContent) {
                VideoContent video = (VideoContent) content;
                text = String.format("[Movie:%s]", video.getFilename());
            } else {
                FileContent file = (FileContent) content;
                text = String.format("[File:%s]", file.getFilename());
            }
        } else if (content instanceof PageContent) {
            // Web page
            PageContent page = (PageContent) content;
            text = String.format("[URL:%s]", page.getURL());
        } else {
            text = String.format("Current version doesn't support this message type: %s", content.getType());
        }
        // store message text
        content.put("text", text);
        return text;
    }

    public static String getCommandText(Command cmd, ID commander) {
        String text = (String) cmd.get("text");
        if (text != null) {
            return text;
        }
        if (cmd instanceof GroupCommand) {
            text = MessageBuilder.getGroupCommandText((GroupCommand) cmd, commander);
            //} else if (cmd instanceof HistoryCommand) {
            // TODO: process history command
        } else if (cmd instanceof LoginCommand) {
            text = MessageBuilder.getLoginCommandText((LoginCommand) cmd, commander);
        } else {
            text = String.format("Current version doesn't support this command: %s", cmd.getCommand());
        }
        // store message text
        cmd.put("text", text);
        return text;
    }

    public static Facebook facebook = null;
}
