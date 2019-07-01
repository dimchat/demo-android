package chat.dim.sechat.model;

import chat.dim.client.Amanuensis;
import chat.dim.database.Conversation;
import chat.dim.client.Facebook;
import chat.dim.core.Barrack;
import chat.dim.database.MessageTable;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.ID;
import chat.dim.protocol.TextContent;

public class MessageProcessor {
    private static final MessageProcessor ourInstance = new MessageProcessor();

    public static MessageProcessor getInstance() {
        return ourInstance;
    }

    private MessageProcessor() {
    }

    static Barrack barrack = Facebook.getInstance();
    static Amanuensis clerk = Amanuensis.getInstance();

    static {

        // test

        ID hulk = ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");
        ID moki = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");

        Content content = new TextContent("Hello world!");
        InstantMessage iMsg = new InstantMessage(content,hulk, moki);

        Conversation chatBox = clerk.getConversation(hulk);
        MessageTable.insertMessage(iMsg, chatBox);
    }
}
