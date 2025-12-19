package com.mock.location;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // 确保布局里有 et_lat, et_lng, btn_save

        EditText etLat = findViewById(R.id.et_lat);
        EditText etLng = findViewById(R.id.et_lng);
        Button btnSave = findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> {
            // 1. 申请定位权限
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);

            // 2. 启动 Fake GPS 同款服务
            try {
                Intent i = new Intent(this, MockLocationService.class);
                i.putExtra("lat", Double.parseDouble(etLat.getText().toString()));
                i.putExtra("lng", Double.parseDouble(etLng.getText().toString()));
                startForegroundService(i);
            } catch (Exception e) {
                // 如果开发者选项没勾选此 App，这里会启动失败，引导去设置
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            }
        });
    }
}