package chat.dim.sechat.settings.station;

import java.util.List;

import chat.dim.database.ProviderTable;
import chat.dim.protocol.ID;
import chat.dim.ui.list.DummyItem;
import chat.dim.ui.list.DummyList;

/**
 * Helper class for providing sample content for user interfaces created by
 * Android template wizards.
 * <p>
 * TODO: Replace all uses of this class before publishing your app.
 */
public class StationList extends DummyList<StationList.Item> {

    @Override
    public void reloadData() {
        clearItems();

        ID sp = StationViewModel.getCurrentProvider();
        if (sp != null) {
            List<ProviderTable.StationInfo> stations = StationViewModel.getStations(sp);
            if (stations != null) {
                for (ProviderTable.StationInfo item : stations) {
                    if (item.chosen == 1) {
                        continue;
                    }
                    addItem(new Item(item.identifier, item.name, item.host, item.port, item.chosen));
                }
            }
        }
    }

    /**
     * A dummy item representing a piece of content.
     */
    public static class Item extends ProviderTable.StationInfo implements DummyItem {

        public Item(ID identifier, String name, String host, int port, int chosen) {
            super(identifier, name, host, port, chosen);
        }
    }
}
