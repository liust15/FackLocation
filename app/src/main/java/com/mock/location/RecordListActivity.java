// com.mock.location.RecordListActivity
package com.mock.location;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mock.location.adapter.RecordListAdapter;
import com.mock.location.model.LocationRecord;
import com.mock.location.util.RecordManager;
import java.util.List;

public class RecordListActivity extends AppCompatActivity {

    private RecordListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);

        RecyclerView recyclerView = findViewById(R.id.recycler_records);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new RecordListAdapter(this, new RecordListAdapter.OnRecordActionListener() {
            @Override
            public void onSetCurrent(int index) {
                RecordManager.setCurrentRecordIndex(RecordListActivity.this, index);
            }

            @Override
            public void onDataSetChanged() {
                loadRecords();
            }
        });

        recyclerView.setAdapter(adapter);
        loadRecords();
    }

    private void loadRecords() {
        List<LocationRecord> records = RecordManager.getAllRecords(this);
        adapter.updateRecords(records);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords(); // 返回时刷新
    }
}