package com.mock.location;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
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
        int id = item.getItemId();
        if (id == R.id.action_add) {
            collectAndAddRecord();
            return true;
        } else if (id == R.id.action_manage) {
            startActivity(new Intent(this, RecordListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void collectAndAddRecord() {
        if (isCollecting) return; // 防止重复触发
        isCollecting = true;

        boolean hasLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
        boolean hasPhoneState = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                == PackageManager.PERMISSION_GRANTED;

        if (hasLocation && hasPhoneState) {
            doCollectLocation();
        } else {
            // 一次性申请定位 + 读取电话状态权限，便于后续采集基站信息
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE},
                    REQUEST_LOCATION_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            boolean hasLocation = false;
            boolean hasPhoneState = false;

            for (int i = 0; i < permissions.length; i++) {
                if (Manifest.permission.ACCESS_FINE_LOCATION.equals(permissions[i])) {
                    hasLocation = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                } else if (Manifest.permission.READ_PHONE_STATE.equals(permissions[i])) {
                    hasPhoneState = grantResults[i] == PackageManager.PERMISSION_GRANTED;
                }
            }

            if (hasLocation && hasPhoneState) {
                // 权限齐备，继续采集
                doCollectLocation();
            } else {
                isCollecting = false;
                Toast.makeText(this, "需要定位和电话状态权限才能记录位置", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static final String TAG = Constant.TAG;

    private void doCollectLocation() {
        Log.d(TAG, "【doCollectLocation】开始请求定位");

        // 显示 Loading 弹窗
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
            Log.w(TAG, "【TIMEOUT】定位请求超时（15秒）");
            runOnUiThread(() -> {
                dismissLoadingDialog();
                Toast.makeText(MainActivity.this, "❌ 获取位置超时，请重试", Toast.LENGTH_LONG).show();
                isCollecting = false;
            });
        };
        handler.postDelayed(timeoutRunnable, 15000);

        Log.d(TAG, "【CALL】调用 RealLocationCollector.collectCurrentLocation(...)");
        RealLocationCollector.collectCurrentLocation(this, new RealLocationCollector.OnLocationCollectedCallback() {
            @Override
            public void onCollected(LocationRecord record) {
                int wifiCount = (record.wifiBssids != null) ? record.wifiBssids.size() : 0;
                boolean hasCellInfo = record.cellInfo != null;
                Log.d(TAG, "【onCollected】lat=" + record.lat + ", lng=" + record.lng
                        + ", wifiCount=" + wifiCount
                        + ", cellInfo=" + (hasCellInfo ? record.cellInfo.toString() : "null"));

                dismissLoadingDialog();
                showSaveDialog(record);
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "【onCollected:onError】" + error);
                dismissLoadingDialog();
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                isCollecting = false;
            }
        });

        Log.d(TAG, "【END】doCollectLocation 执行完毕（已发起定位请求）");
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

        int wifiCount = (record.wifiBssids != null) ? record.wifiBssids.size() : 0;
        String cellInfoPreview;
        if (record.cellInfo != null && record.cellInfo.networkType != null) {
            cellInfoPreview = record.cellInfo.networkType
                    + "/" + record.cellInfo.mcc
                    + "/" + record.cellInfo.mnc
                    + "/" + record.cellInfo.lac
                    + "/" + record.cellInfo.cid;
        } else {
            cellInfoPreview = "无";
        }
        String preview = String.format("坐标: %.6f, %.6f\nWiFi BSSID 数量: %d\n基站信息: %s",
                record.lat, record.lng, wifiCount, cellInfoPreview);
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
            isCollecting = false;
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
        if (!canUseMockLocation(this)) {
            return;
        }
        boolean success = ConfigFileUtil.writeObject(this, current);
        if (success) {
            MockLocationHelper.startMockingLocation(this, current.getLat(), current.getLng());
            Toast.makeText(this, "✅ Mock 定位已生效！", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ 写入失败（需 Root + Xposed）", Toast.LENGTH_SHORT).show();
        }
    }
    public boolean canUseMockLocation(Context context) {
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        String provider = "test_mock_provider";

        try {

            lm.addTestProvider(
                    provider,
                    false, // requiresNetwork
                    false, // requiresSatellite
                    false, // requiresCell
                    false, // hasMonetaryCost
                    true,  // supportsAltitude
                    true,  // supportsSpeed
                    true,  // supportsBearing
                    Criteria.POWER_LOW,      // ✅ 必须是 POWER_LOW (1), POWER_MEDIUM (2), 或 POWER_HIGH (3)
                    Criteria.ACCURACY_FINE   // ✅ ACCURACY_COARSE (2) 或 ACCURACY_FINE (1)
            );
            lm.removeTestProvider(provider); // 立即清理
            return true; // 成功 = 有权限
        } catch (SecurityException e) {
            return false; // 没有被设为 Mock App
        } catch (Exception e) {
            return false;
        }
    }


    private void cancelMock() {
        boolean deleted = ConfigFileUtil.deleteMockFile(this);
        MockLocationHelper.stopMockingLocation(this);
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