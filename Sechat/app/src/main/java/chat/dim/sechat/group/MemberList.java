package chat.dim.sechat.group;

import java.util.List;

import chat.dim.client.Facebook;
import chat.dim.client.Messenger;
import chat.dim.protocol.ID;

public class MemberList extends CandidateList {

    private final ID group;

    public MemberList(ID identifier) {
        super(identifier);
        group = identifier;
    }

    @Override
    public synchronized void reloadData() {
        clearItems();

        Facebook facebook = Messenger.getInstance().getFacebook();
        List<ID> members = facebook.getMembers(group);
        if (members != null) {
            for (ID member : members) {
                addItem(new CandidateList.Item(member));
            }
        }
    }
}
