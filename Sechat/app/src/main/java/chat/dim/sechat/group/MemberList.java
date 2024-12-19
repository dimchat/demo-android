package chat.dim.sechat.group;

import java.util.List;

import chat.dim.group.SharedGroupManager;
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

        SharedGroupManager manager = SharedGroupManager.getInstance();
        List<ID> members = manager.getMembers(group);
        if (members != null) {
            for (ID member : members) {
                addItem(new CandidateList.Item(member));
            }
        }
    }
}
