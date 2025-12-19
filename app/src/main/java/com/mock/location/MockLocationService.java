package com.mock.location;

import android.app.*;
import android.content.*;
import android.location.*;
import android.os.*;

public class MockLocationService extends Service {
    private LocationManager lm;
    private double lat, lng;
    private Handler handler = new Handler(Looper.getMainLooper());

    private Runnable mockTask = new Runnable() {
        @Override
        public void run() {
            try {
                // 模拟一个 GPS 信号
                Location loc = new Location(LocationManager.GPS_PROVIDER);
                loc.setLatitude(lat);
                loc.setLongitude(lng);
                loc.setAltitude(10.0);
                loc.setAccuracy(1.0f);
                loc.setTime(System.currentTimeMillis());
                loc.setElapsedRealtimeNanos(SystemClock.elapsedRealtimeNanos());

                // 核心：像 Fake GPS 一样通过官方接口推流
                lm.setTestProviderLocation(LocationManager.GPS_PROVIDER, loc);
            } catch (Exception ignored) {}
            handler.postDelayed(this, 1000); // 持续推流，防止被系统纠偏
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        try {
            // 这步就是 Fake GPS 在“开发者选项”选中后干的事：注册自己为 GPS 供应商
            lm.addTestProvider(LocationManager.GPS_PROVIDER, false, false, false, false, true, true, true, 3, 1);
            lm.setTestProviderEnabled(LocationManager.GPS_PROVIDER, true);
        } catch (Exception e) {
            stopSelf();
        }

        NotificationChannel chan = new NotificationChannel("c", "LocService", NotificationManager.IMPORTANCE_LOW);
        getSystemService(NotificationManager.class).createNotificationChannel(chan);
        startForeground(101, new Notification.Builder(this, "c").setContentTitle("定位模拟中").setSmallIcon(android.R.drawable.ic_menu_mylocation).build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            lat = intent.getDoubleExtra("lat", 39.9);
            lng = intent.getDoubleExtra("lng", 116.4);
            handler.post(mockTask);
        }
        return START_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}