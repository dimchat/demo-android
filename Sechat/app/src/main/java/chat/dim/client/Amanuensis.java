package chat.dim.client;

import java.util.HashMap;
import java.util.Map;

import chat.dim.core.Barrack;
import chat.dim.database.Conversation;
import chat.dim.database.MessageTable;
import chat.dim.dkd.InstantMessage;
import chat.dim.mkm.entity.Address;
import chat.dim.mkm.entity.Entity;
import chat.dim.mkm.entity.ID;

public class Amanuensis {
    private static final Amanuensis ourInstance = new Amanuensis();

    public static Amanuensis getInstance() {
        return ourInstance;
    }

    private Amanuensis() {
    }

    private Map<Address, Conversation> conversationMap = new HashMap<>();

    // conversation factory
    public Conversation getConversation(ID identifier) {
        Conversation chatBox = conversationMap.get(identifier.address);
        if (chatBox != null) {
            return chatBox;
        }
        // create directly if we can find the entity
        Barrack barrack = Facebook.getInstance();
        Entity entity;
        if (identifier.getType().isCommunicator()) {
            entity = barrack.getAccount(identifier);
        } else if (identifier.getType().isGroup()) {
            entity = barrack.getGroup(identifier);
        } else {
            entity = null;
        }
        if (entity == null) {
            throw new NullPointerException("failed to create conversation:" + identifier);
        }
        chatBox = new Conversation(entity);
        cacheConversation(chatBox);
        return chatBox;
    }

    public void cacheConversation(Conversation chatBox) {
        conversationMap.put(chatBox.identifier.address, chatBox);
    }

    public void removeConversation(Conversation chatBox) {
        conversationMap.remove(chatBox.identifier.address);
    }

    //---- Message

    /**
     *  Save received message
     *
     * @param iMsg - instant message
     * @return true on success
     */
    public boolean saveMessage(InstantMessage iMsg) {
        ID sender = ID.getInstance(iMsg.envelope.sender);
        ID receiver = ID.getInstance(iMsg.envelope.receiver);
        ID group = ID.getInstance(iMsg.content.getGroup());

        Conversation chatBox;
        if (receiver.getType().isGroup()) {
            // group chat, get chat box with group ID
            chatBox = getConversation(receiver);
        } else if (group != null) {
            // group chat, get chat box with group ID
            chatBox = getConversation(group);
        } else {
            // personal chat, get chat box with contact ID
            chatBox = getConversation(sender);
        }
        return MessageTable.insertMessage(iMsg, chatBox);
    }

    /**
     *  Update message state with receipt
     *
     * @param iMsg - receipt message
     * @return YES while target message found
     */
    public boolean saveReceipt(InstantMessage iMsg) {
        // TODO:
        return true;
    }
}
