package chat.dim.sechat.group;

import java.util.List;

import chat.dim.model.Facebook;
import chat.dim.protocol.ID;

public class MemberList extends CandidateList {

    private static Facebook facebook = Facebook.getInstance();

    private final ID group;

    public MemberList(ID identifier) {
        super(identifier);
        group = identifier;
    }

    @Override
    public synchronized void reloadData() {
        clearItems();

        List<ID> members = facebook.getMembers(group);
        if (members != null) {
            for (ID member : members) {
                addItem(new CandidateList.Item(member));
            }
        }
    }
}
