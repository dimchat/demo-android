package chat.dim.sechat.settings.station;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import chat.dim.sechat.R;
import chat.dim.sechat.model.EntityViewModel;
import chat.dim.ui.list.Listener;
import chat.dim.ui.list.RecyclerViewAdapter;
import chat.dim.ui.list.RecyclerViewHolder;

public class StationViewAdapter extends RecyclerViewAdapter<StationViewAdapter.ViewHolder, StationList> {

    StationViewAdapter(StationList list, Listener<ViewHolder> listener) {
        super(list, listener);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.setting_station_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StationList.Item item = dummyList.getItem(position);

        String host = item.host;
        String port = "" + item.port;
        String name = EntityViewModel.getName(item.identifier);

        holder.hostView.setText(host);
        holder.portView.setText(port);
        holder.nameView.setText(name);

        holder.chooseButton.setOnClickListener(v -> choose(item));
        holder.deleteButton.setOnClickListener(v -> delete(item));

        super.onBindViewHolder(holder, position);
    }

    private void choose(StationList.Item item) {
        StationViewModel.chooseStation(item.identifier);
    }

    private void delete(StationList.Item item) {
        StationViewModel.deleteStation(item.identifier, item.host, item.port);
    }

    public static class ViewHolder extends RecyclerViewHolder<StationList.Item> {

        final TextView hostView;
        final TextView portView;
        final TextView nameView;

        final View chooseButton;
        final View deleteButton;

        public ViewHolder(View view) {
            super(view);
            hostView = view.findViewById(R.id.station_host);
            portView = view.findViewById(R.id.station_port);
            nameView = view.findViewById(R.id.station_name);

            chooseButton = view.findViewById(R.id.choose);
            deleteButton = view.findViewById(R.id.delete);
        }
    }
}
