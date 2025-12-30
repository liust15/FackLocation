package com.mock.location;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.mock.location.adapter.RecordSelectAdapter;
import com.mock.location.model.LocationRecord;
import com.mock.location.util.ConfigFileUtil;
import com.mock.location.util.RecordManager;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_LOCATION_PERMISSION = 100;
    private AlertDialog loadingDialog;
    private boolean isCollecting = false; // 防止重复点击

    private RecordSelectAdapter adapter;
    private Button btnStartMock, btnCancelMock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.recycler_records);
        btnStartMock = findViewById(R.id.btn_start_mock);
        btnCancelMock = findViewById(R.id.btn_cancel_mock);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecordSelectAdapter(this, position -> {
            btnStartMock.setEnabled(true);
        });
        recyclerView.setAdapter(adapter);

        loadRecords();

        btnStartMock.setOnClickListener(v -> startMock());
        btnCancelMock.setOnClickListener(v -> cancelMock());

        // 启动时检查权限（非必须，因为 collectAndAddRecord 也会检查）
        checkPermissionsOnStart();
    }

    private void checkPermissionsOnStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 不强制请求，等用户点击 "+" 时再申请
        }
    }

    // ===== 菜单：右上角 + =====
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_add) {
            collectAndAddRecord();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void collectAndAddRecord() {
        if (isCollecting) return; // 防止重复触发
        isCollecting = true;

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            doCollectLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            isCollecting = false;
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                doCollectLocation();
            } else {
                Toast.makeText(this, "需要定位权限才能记录位置", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void doCollectLocation() {
        // 显示 Loading 弹窗（Landing 页面）
        if (loadingDialog == null || !loadingDialog.isShowing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("正在获取当前位置");
            builder.setMessage("请稍候...\n确保 GPS 或网络定位已开启");
            builder.setCancelable(false);
            builder.setView(R.layout.dialog_loading);
            loadingDialog = builder.create();
            loadingDialog.show();
        }

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            runOnUiThread(() -> {
                dismissLoadingDialog();
                Toast.makeText(MainActivity.this, "❌ 获取位置超时，请重试", Toast.LENGTH_LONG).show();
                isCollecting = false;
            });
        };
        handler.postDelayed(timeoutRunnable, 15000); // 15秒超时

        RealLocationCollector.collectCurrentLocation(this, new RealLocationCollector.OnLocationCollectedCallback() {
            @Override
            public void onCollected(LocationRecord record) {
                handler.removeCallbacks(timeoutRunnable);
                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    showSaveDialog(record);
                    isCollecting = false;
                });
            }

            @Override
            public void onError(String error) {
                handler.removeCallbacks(timeoutRunnable);
                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    Toast.makeText(MainActivity.this, "❌ " + error, Toast.LENGTH_LONG).show();
                    isCollecting = false;
                });
            }
        });
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
        loadingDialog = null;
    }

    private void showSaveDialog(LocationRecord record) {
        List<LocationRecord> existing = RecordManager.getAllRecords(this);
        String defaultName = "记录" + (existing.size() + 1);

        TextInputEditText input = new TextInputEditText(this);
        input.setText(defaultName);
        input.setHint("输入记录名称");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("保存新记录");
        String preview = String.format("位置: %.4f, %.4f", record.lat, record.lng);
        builder.setMessage(preview);
        builder.setView(input);

        builder.setPositiveButton("确认", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) name = defaultName;
            record.name = name;
            record.timestamp = System.currentTimeMillis();

            RecordManager.addRecord(this, record);
            loadRecords();
            Toast.makeText(this, "✅ 已保存：" + name, Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("取消", (dialog, which) -> {
            isCollecting = false;
        });

        builder.setOnCancelListener(dialog -> isCollecting = false);
        builder.show();
    }

    private void loadRecords() {
        List<LocationRecord> records = RecordManager.getAllRecords(this);
        adapter.updateRecords(records);
        int currentIndex = RecordManager.getCurrentRecordIndex(this);
        adapter.setSelectedIndex(currentIndex);
        btnStartMock.setEnabled(currentIndex >= 0);
    }

    private void startMock() {
        LocationRecord current = RecordManager.getCurrentRecord(this);
        if (current == null) return;

        boolean success = ConfigFileUtil.writeObject(this, current);
        if (success) {
            Toast.makeText(this, "✅ Mock 定位已生效！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ 写入失败（需 Root + Xposed）", Toast.LENGTH_SHORT).show();
        }
    }

    private void cancelMock() {
        boolean deleted = ConfigFileUtil.deleteMockFile(this);
        if (deleted) {
            Toast.makeText(this, "⏹️ 模拟已停止", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "ℹ️ 无模拟配置", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecords(); // 刷新列表
    }
}