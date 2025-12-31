// com.mock.location.adapter.RecordListAdapter
package com.mock.location.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mock.location.R;
import com.mock.location.model.LocationRecord;
import com.mock.location.util.RecordManager;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class RecordListAdapter extends RecyclerView.Adapter<RecordListAdapter.ViewHolder> {

    private final Context context;
    private List<LocationRecord> records;
    private final OnRecordActionListener listener;

    public interface OnRecordActionListener {
        void onSetCurrent(int index);
        void onDataSetChanged();
    }

    public RecordListAdapter(Context context, OnRecordActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void updateRecords(List<LocationRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_record, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LocationRecord record = records.get(position);

        holder.tvName.setText(record.name);
        holder.tvCoords.setText(String.format("(%.4f, %.4f)", record.lat, record.lng));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        holder.tvTime.setText(sdf.format(record.timestamp));

        // 获取当前选中索引
        int currentIndex = getCurrentIndex();

        // 高亮当前记录
        if (position == currentIndex) {
            holder.tvName.setTextColor(0xFF2196F3); // 蓝色
        } else {
            holder.tvName.setTextColor(0xFF000000); // 黑色
        }

        holder.btnSetCurrent.setOnClickListener(v -> {
            listener.onSetCurrent(position);
            notifyItemChanged(currentIndex); // 刷新旧的
            notifyItemChanged(position);      // 刷新新的
        });

        holder.btnDelete.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("确认删除")
                    .setMessage("确定要删除“" + record.name + "”吗？")
                    .setPositiveButton("删除", (dialog, which) -> {
                        records.remove(position);
                        RecordManager.saveAllRecords(context, records);

                        // 如果删除的是当前记录，重置当前索引
                        if (position == currentIndex) {
                            RecordManager.setCurrentRecordIndex(context, -1);
                        } else if (currentIndex > position) {
                            // 索引前移
                            RecordManager.setCurrentRecordIndex(context, currentIndex - 1);
                        }

                        listener.onDataSetChanged();
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private int getCurrentIndex() {
        List<LocationRecord> all = RecordManager.getAllRecords(context);
        int currentIndex = RecordManager.getCurrentRecordIndex(context);
        // 安全校验
        if (currentIndex >= 0 && currentIndex < all.size()) {
            // 找到当前记录在 records 中的位置（可能已被过滤）
            for (int i = 0; i < records.size(); i++) {
                if (records.get(i).timestamp == all.get(currentIndex).timestamp) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int getItemCount() {
        return records == null ? 0 : records.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCoords, tvTime;
        Button btnSetCurrent, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvCoords = itemView.findViewById(R.id.tv_coords);
            tvTime = itemView.findViewById(R.id.tv_time);
            btnSetCurrent = itemView.findViewById(R.id.btn_set_current);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}