package chat.dim.client;

import chat.dim.dkd.InstantMessage;

public interface ConversationDataSource {

    /**
     *  Get message count in this conversation for an entity
     *
     * @param chatBox - conversation instance
     * @return total count
     */
    int numberOfMessages(Conversation chatBox);

    /**
     *  Get message at index of this conversation
     *
     * @param index - start from 0, latest first
     * @param chatBox - conversation instance
     * @return instant message
     */
    InstantMessage messageAtIndex(int index, Conversation chatBox);

    /**
     *  Save the new message to local storage
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    boolean insertMessage(InstantMessage iMsg, Conversation chatBox);

    /**
     *  Delete the message
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    boolean removeMessage(InstantMessage iMsg, Conversation chatBox);

    /**
     *  Try to withdraw the message, maybe won't success
     *
     * @param iMsg - instant message
     * @param chatBox - conversation instance
     * @return true on success
     */
    boolean withdrawMessage(InstantMessage iMsg, Conversation chatBox);
}
