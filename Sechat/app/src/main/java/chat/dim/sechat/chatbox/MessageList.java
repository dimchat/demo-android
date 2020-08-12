package chat.dim.sechat.chatbox;

import java.util.ArrayList;
import java.util.List;

import chat.dim.InstantMessage;
import chat.dim.model.Conversation;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

public class MessageList extends DummyList<MessageList.Item> {

    final Conversation chatBox;
    private ChatboxViewModel viewModel = null;

    MessageList(Conversation chatBox) {
        super();
        this.chatBox = chatBox;
    }

    void setViewModel(ChatboxViewModel viewModel) {
        this.viewModel = viewModel;
    }

    @Override
    public synchronized void reloadData() {
        if (viewModel == null) {
            return;
        }
        clearItems();

        List<InstantMessage> messages = new ArrayList<>(viewModel.getMessages(chatBox));
        for (InstantMessage msg : messages) {
            addItem(new Item(msg));
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item implements DummyItem {

        final InstantMessage msg;

        public Item(InstantMessage iMsg) {
            msg = iMsg;
        }
    }
}
