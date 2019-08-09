package chat.dim.database;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.client.ConversationDataSource;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.ID;

public class ConversationDatabase implements ConversationDataSource {
    private static final ConversationDatabase ourInstance = new ConversationDatabase();
    public static ConversationDatabase getInstance() { return ourInstance; }
    private ConversationDatabase() {
        // initialized delegate of clerk
        Amanuensis clerk = Amanuensis.getInstance();
        clerk.conversationDataSource = this;
    }

    public void reloadData(ID user) {
        // reload conversations for current user
        ConversationTable.reloadData(user);
    }

    //-------- ConversationDataSource

    @Override
    public int numberOfConversations() {
        return ConversationTable.numberOfConversations();
    }

    @Override
    public Conversation conversationAtIndex(int index) {
        return ConversationTable.conversationAtIndex(index);
    }

    @Override
    public ID removeConversationAtIndex(int index) {
        return ConversationTable.removeConversationAtIndex(index);
    }

    @Override
    public boolean removeConversation(Conversation chatBox) {
        return ConversationTable.removeConversation(chatBox);
    }

    // messages

    @Override
    public int numberOfMessages(Conversation chatBox) {
        return MessageTable.numberOfMessages(chatBox);
    }

    @Override
    public InstantMessage messageAtIndex(int index, Conversation chatBox) {
        return MessageTable.messageAtIndex(index, chatBox);
    }

    @Override
    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        return MessageTable.insertMessage(iMsg, chatBox);
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        return MessageTable.removeMessage(iMsg, chatBox);
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        return MessageTable.withdrawMessage(iMsg, chatBox);
    }

    @Override
    public boolean saveReceipt(InstantMessage receipt, Conversation chatBox) {
        return MessageTable.saveReceipt(receipt, chatBox);
    }
}
