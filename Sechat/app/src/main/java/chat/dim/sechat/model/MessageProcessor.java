package chat.dim.sechat.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import chat.dim.client.Amanuensis;
import chat.dim.client.Conversation;
import chat.dim.client.ConversationDataSource;
import chat.dim.client.Facebook;
import chat.dim.core.Barrack;
import chat.dim.dkd.Content;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.Entity;
import chat.dim.mkm.entity.ID;
import chat.dim.protocol.TextContent;

public class MessageProcessor implements ConversationDataSource {
    private static final MessageProcessor ourInstance = new MessageProcessor();

    public static MessageProcessor getInstance() {
        return ourInstance;
    }

    private MessageProcessor() {
        reloadData();
        clerk.dataSource = this;
    }

    private Barrack barrack = Facebook.getInstance();
    private Amanuensis clerk = Amanuensis.getInstance();

    private Map<ID, List<InstantMessage>> chatHistory;
    private List<ID> chatList;
    private Map<ID, List<String>> timesTable;

    private boolean reloadData() {
        setChatHistory(scanMessages());
        return true;
    }

    private Map<ID, List<InstantMessage>> scanMessages() {
        // TODO: load messages from "Documents/.dim/{Address}/messages.js"
        return new HashMap<>();
    }

    private boolean removeMessages(ID identifier) {
        // TODO: remove message file with ID
        return true;
    }

    private boolean clearMessages(ID identifier) {
        // TODO: empty message file with ID
        return true;
    }

    private void setChatHistory(Map<ID, List<InstantMessage>> dictionary) {
        chatHistory = dictionary;
        chatList = new ArrayList<>(dictionary.keySet());
        sortConversationList();
        timesTable = new HashMap<>();
    }

    private void sortConversationList() {
        // TODO: get last time of each conversations and sort chatList
    }

    // Conversation factory
    Conversation getConversation(ID identifier) {
        Entity entity = null;
        if (identifier.getType().isCommunicator()) {
            entity = barrack.getAccount(identifier);
        } else if (identifier.getType().isGroup()) {
            entity = barrack.getGroup(identifier);
        }
        assert entity != null;
        Conversation chatBox = new Conversation(entity);
        chatBox.dataSource = this;
        return chatBox;
    }

    //----

    public int numberOfConversations() {
        return chatList.size();
    }

    public Conversation conversationAtIndex(int index) {
        return clerk.getConversation(chatList.get(index));
    }

    public boolean removeConversationAtIndex(int index) {
        return removeConversation(conversationAtIndex(index));
    }

    public boolean removeConversation(Conversation chatBox) {
        ID identifier = chatBox.identifier;
        boolean removed = removeMessages(identifier);
        if (removed) {
            chatHistory.remove(identifier);
            chatList.remove(identifier);
            // TODO: post notification "MessageCleaned" with user info: {"ID": identifier}
        }
        return removed;
    }

    public boolean clearConversationAtIndex(int index) {
        return clearConversation(conversationAtIndex(index));
    }

    public boolean clearConversation(Conversation chatBox) {
        ID identifier = chatBox.identifier;
        boolean cleared = clearMessages(identifier);
        if (cleared) {
            List<InstantMessage> history = chatHistory.get(identifier);
            if (history != null) {
                history.clear();
                // TODO: post notification "MessageCleaned" with user info: {"ID": identifier}
            }
        }
        return cleared;
    }

    //---- ConversationDataSource

    @Override
    public int numberOfMessages(Conversation chatBox) {
        List<InstantMessage> history = chatHistory.get(chatBox.identifier);
        return history == null ? 0 : history.size();
    }

    @Override
    public InstantMessage messageAtIndex(int index, Conversation chatBox) {
        List<InstantMessage> history = chatHistory.get(chatBox.identifier);
        assert history != null;
        InstantMessage iMsg = InstantMessage.getInstance(history.get(index));
        // TODO: get 'timeTag' for this message
        assert iMsg != null;
        return iMsg;
    }

    @Override
    public boolean insertMessage(InstantMessage iMsg, Conversation chatBox) {
        // TODO: save new message
        return false;
    }

    @Override
    public boolean removeMessage(InstantMessage iMsg, Conversation chatBox) {
        return false;
    }

    @Override
    public boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox) {
        return false;
    }

    static {
        MessageProcessor database = MessageProcessor.getInstance();

        // test
        Content content;
        InstantMessage iMsg;

        List<InstantMessage> history = new ArrayList<>();

        ID hulk = ID.getInstance("hulk@4YeVEN3aUnvC1DNUufCq1bs9zoBSJTzVEj");
        ID moki = ID.getInstance("moki@4WDfe3zZ4T7opFSi3iDAKiuTnUHjxmXekk");

        content = new TextContent("Hello world!");
        iMsg = new InstantMessage(content,hulk, moki);
        history.add(iMsg);

        database.chatHistory.put(hulk, history);
        database.chatList.add(hulk);
    }
}
