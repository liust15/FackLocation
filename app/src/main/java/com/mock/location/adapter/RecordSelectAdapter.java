// RecordSelectAdapter.java
package com.mock.location.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.mock.location.R;
import com.mock.location.model.LocationRecord;
import com.mock.location.util.RecordManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RecordSelectAdapter extends RecyclerView.Adapter<RecordSelectAdapter.ViewHolder> {

    private final Context context;
    private List<LocationRecord> records = new ArrayList<>();
    private int selectedIndex = -1;
    private final OnItemSelectedListener listener;

    public interface OnItemSelectedListener {
        void onItemSelected(int index);
    }

    public RecordSelectAdapter(Context context, OnItemSelectedListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateRecords(List<LocationRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    public void setSelectedIndex(int index) {
        this.selectedIndex = index;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_record_selectable, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        LocationRecord record = records.get(position);

        holder.tvName.setText(record.name);
        holder.tvCoords.setText(String.format("(%.4f, %.4f)", record.lat, record.lng));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(record.timestamp));

        // 选中高亮
        if (position == selectedIndex) {
            holder.indicator.setVisibility(View.VISIBLE);
        } else {
            holder.indicator.setVisibility(View.INVISIBLE);
        }

        holder.itemView.setOnClickListener(v -> {
            int oldIndex = selectedIndex;
            selectedIndex = position;
            notifyItemChanged(oldIndex);
            notifyItemChanged(position);
            RecordManager.setCurrentRecordIndex(context, position);
            listener.onItemSelected(position);
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View indicator;
        TextView tvName, tvCoords, tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            indicator = itemView.findViewById(R.id.view_selected_indicator);
            tvName = itemView.findViewById(R.id.tv_name);
            tvCoords = itemView.findViewById(R.id.tv_coords);
            tvTime = itemView.findViewById(R.id.tv_time);
        }
    }
}