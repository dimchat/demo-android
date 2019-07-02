package chat.dim.common;

import chat.dim.mkm.Group;
import chat.dim.mkm.entity.ID;

public class Polylogue extends Group {

    public Polylogue(ID identifier) {
        super(identifier);
    }

    @Override
    public ID getOwner() {
        // polylogue's owner is founder
        ID owner = super.getOwner();
        if (owner != null && owner.isValid()) {
            //assert owner == getFounder();
            return owner;
        }
        return getFounder();
    }
}
